# SOMA-220 최초 가입 시 닉네임 자동 설정 Plan

## 요구사항 확정

- 닉네임은 **공백 포함 6자**, `uk_users_nickname` **unique**
- 포맷: `형용사(2자) + 명사(2자) + 숫자(2자)` — 예) `행복토끼07`
  - 형용사 50 × 명사 50 × 00~99 = **250,000 조합**
- 닉네임은 **User 엔티티 생성 시점에 채워서 INSERT에 실는다** (후속 UPDATE 없음)
- 중복 회피: 랜덤 생성 + 조회 재시도, 최종 방어선은 DB unique 제약 + 상위 트랜잭션 재시도
- 기존 `nickname IS NULL` 유저 백필은 **이번 범위 제외** (별도 티켓)
- 응답 스펙 변경 없음 — 기존 `GET /api/users/me`, `/api/users/profile`로 조회

## 현재 코드 사실 관계

- `User.register(provider, providerId, email)`는 nickname을 **null**로 둔다 (`User.java:71`)
- `User.changeNickname`이 trim + 1~6자 검증을 이미 담당 (`User.java:114`)
- `UserRegistrationService.register()`가 저장 + 농장 초기화를 수행 (`UserRegistrationService.java:41`)
- `@GeneratedValue(IDENTITY)`라 `save()`가 즉시 INSERT를 실행한다 → **unique 위반이 `save()` 그 자리에서 발생**
- 탈퇴 시 `nickname = null`로 비우므로 소프트삭제 유저는 닉네임을 점유하지 않는다 (MySQL은 NULL 중복 허용)
- 재사용 가능한 기존 자산: `RandomProvider`(`common/random`), `UserRepository.findUserByNickname`
- 테스트 DB는 H2 인메모리 `testdb`를 `DB_CLOSE_DELAY=-1`로 **여러 `@SpringBootTest` 클래스가 공유**한다

## 변경 대상

- 추가: `user/domain/NicknameGenerator.java`, `NicknameGeneratorTest`
- 수정: `User`, `UserNicknameService`, `UserRegistrationService`, `SocialLoginService`
- 테스트 수정: `User.register(` 호출부 23개 파일 56곳 + `UserNicknameServiceTest`, `UserRegistrationServiceTest`, `SocialLoginServiceTest`
- DB 변경: **없음** (컬럼·제약 그대로, 마이그레이션 불필요)

## 구현 순서

### 1. Domain — `user/domain/NicknameGenerator.java`

- `@Component`, `RandomProvider` 생성자 주입 (`capture/domain/TierSelectionPolicy`와 동일한 기존 패턴)
- `private static final List<String> ADJECTIVES` (2자 50개), `NOUNS` (2자 50개)
- `public String generate()` → `형용사 + 명사 + %02d` 로 항상 정확히 6자
- 단어는 게임 톤에 맞춰 선정하고, 형용사×명사 2,500 조합에 어색·부적절한 조합이 없는지 목록 확정 시 한 번 훑는다
- verify: `NicknameGeneratorTest` — `RandomProvider` mock으로 인덱스 고정해 결과 문자열 검증, 숫자 zero-padding, 전 조합 6자·공백 없음, 단어 목록 전부 2자

### 2. Domain — `User.register` 시그니처 변경

```
private User(SocialAccount socialAccount, String email, String nickname) {
  this.socialAccount = socialAccount;
  this.email = email;
  this.level = INITIAL_LEVEL;
  this.coins = Coin.of(INITIAL_COINS);
  changeNickname(nickname);
}

public static User register(
    SocialProvider provider, String providerId, String email, String nickname) {
  return new User(new SocialAccount(provider, providerId), email, nickname);
}
```

- 생성자가 `changeNickname`을 호출해 **길이·공백 검증을 한 곳에서** 재사용한다
- "가입 시 닉네임 필수"가 도메인 불변식으로 강제되고, nickname null인 User를 저장할 경로가 사라진다
- verify: `UserTest` — 가입 직후 nickname이 세팅되는지, 7자 이상이면 `INVALID_NICKNAME`

### 3. Test 호출부 마이그레이션 (56곳)

- 순수 단위 테스트 45곳(12개 파일)은 영속화하지 않으므로 **동일 리터럴** 사용 가능
- `@SpringBootTest` 11개 파일은 파일당 호출이 1건씩이고 H2를 공유하므로 **서로 다른 닉네임 리터럴**을 준다
  - `UserWithdrawalPersistenceTest`, `CouponRedeemServiceIntegrationTest`, `CaptureRepositoryOverviewTest`, `CapturePersistenceTest`, `CaptureStartConcurrencyTest`, `CaptureGameResultConcurrencyTest`, `CaptureAttemptPurchaseServiceIntegrationTest`, `CaptureAnimalServiceIntegrationTest`, `AnimalSlotMoveConcurrencyTest`, `AchievementQueryServiceIntegrationTest`, `AchievementClaimServiceIntegrationTest`
- verify: `./gradlew test` 전체 통과 — `uk_users_nickname` 위반이 나면 리터럴 중복

### 4. Application — `UserNicknameService.generateUnique()`

```
public String generateUnique() {
  for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {   // 10회
    String candidate = nicknameGenerator.generate();
    if (userRepository.findUserByNickname(candidate).isEmpty()) {
      return candidate;
    }
  }
  return nicknameGenerator.generate();   // 최종 방어선은 DB unique 제약
}
```

- 닉네임 관심사가 이미 이 서비스에 응집돼 있으므로 새 서비스를 만들지 않는다
- 조회는 기존 `findUserByNickname` 재사용 (신규 리포지토리 메서드 없음)
- `@Transactional`을 붙이지 않고 **호출자 트랜잭션에 참여**시킨다
- 10회 소진 시 경고 로그를 남긴다 (조합 고갈 감지용)
- verify: `UserNicknameServiceTest` — 첫 후보 중복 시 다음 후보 반환, 10회 소진 경로

### 5. Application — `UserRegistrationService.register()`

```
private User register(SocialUserInfo userInfo) {
  String nickname = userNicknameService.generateUnique();
  User user = userRepository.save(
      User.register(userInfo.provider(), userInfo.providerId(), userInfo.email(), nickname));
  farmInitializationService.initialize(user.getId());
  return user;
}
```

- **기존 `catch (DataIntegrityViolationException) → USER_ALREADY_REGISTERED`를 제거**한다
  - 이유: 이제 이 지점에서 소셜 계정 위반과 닉네임 위반이 **둘 다** 터질 수 있어 원인을 구분할 수 없다. 재시도 계층에서 일관되게 처리한다.
  - 위반이 발생하면 영속성 컨텍스트가 오염되고 트랜잭션이 rollback-only가 되므로 **같은 트랜잭션 안에서는 재시도할 수 없다.**
- verify: `UserRegistrationServiceTest` — 신규 가입 시 nickname이 세팅되는지, unique 위반이 그대로 전파되는지

### 6. Application — `SocialLoginService` 재시도

```
private UserRegistration getOrRegister(SocialUserInfo userInfo) {
  for (int attempt = 0; attempt < MAX_REGISTRATION_ATTEMPTS; attempt++) {   // 3회
    try {
      return userRegistrationService.getOrRegister(userInfo);
    } catch (DataIntegrityViolationException ignored) {
    }
  }
  throw new BusinessException(ErrorCode.USER_ALREADY_REGISTERED);
}
```

- `login()`은 비트랜잭션이라 `getOrRegister` 호출마다 **새 트랜잭션**이 열린다 → 오염된 영속성 컨텍스트를 재사용하지 않는다
- 닉네임 경합이면 새 닉네임으로 재생성되고, 소셜 계정 경합이면 2회차 `findBySocialAccount`가 상대 트랜잭션이 커밋한 유저를 찾아 정상 로그인된다
- verify: `SocialLoginServiceTest` — 1회 위반 후 재시도 성공, 3회 모두 실패 시 `USER_ALREADY_REGISTERED`

### 7. Test 정리

| 테스트 | 검증 |
| --- | --- |
| `NicknameGeneratorTest` (신규) | 포맷·길이·zero-padding·단어 목록 무결성 |
| `UserTest` | 가입 시 nickname 세팅, 길이 초과 거부 |
| `UserNicknameServiceTest` | `generateUnique` 중복 시 재시도, 시도 소진 경로 |
| `UserRegistrationServiceTest` | 신규 가입 시 nickname 세팅, unique 위반 전파 |
| `SocialLoginServiceTest` | 재시도 성공 / 재시도 소진 |

- 동시 가입 경합 통합 테스트(`CaptureStartConcurrencyTest` 패턴)는 비용이 커서 우선 제외하고, 필요하면 별도로 제안

## 리스크

- **테스트 호출부 56곳 수정**이 이번 작업의 가장 큰 diff다. 기능 변경은 없지만 리뷰 시 노이즈가 크므로 커밋을 분리한다.
- **앞으로의 함정**: DB 테스트에 유저를 추가할 때 닉네임 리터럴이 겹치면 무관해 보이는 `uk_users_nickname` 위반이 난다. 마이그레이션 시 파일별로 구분되는 값을 넣어 둔다.
- **기존 동작 변경**: 소셜 계정 경합 시 기존에는 409 `USER_ALREADY_REGISTERED`였지만 이제 재시도로 흡수돼 정상 로그인된다. 클라이언트 입장에서는 개선이지만 스펙 변화이므로 공유 필요.
- **롤백 재실행**: 재시도 시 유저 insert와 `FarmInitializationService.initialize`가 함께 롤백된 뒤 재실행된다. `FarmSpace` 저장도 롤백 대상이라 잔여 데이터는 남지 않는다.
- **조합 고갈**: 25만 조합이라 현 규모에서는 문제없다. 10회 소진 로그를 모니터링 지표로 삼는다.
- **인증/인가 영향 없음**: 토큰 발급 경로와 시큐리티 설정은 건드리지 않는다.
- **기존 null 닉네임 유저**: 이번 범위 제외 상태로 남는다. `GET /api/users/me`가 null nickname을 계속 내려줄 수 있음을 FE에 확인.

## 검증

- `./gradlew spotlessApply`
- `./gradlew test`
- `./gradlew check`
- 수동: 신규 소셜 로그인 → `GET /api/users/me`에서 6자 닉네임 확인, 재로그인 시 닉네임이 재생성되지 않는지 확인
