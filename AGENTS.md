# AGENTS.md

이 저장소의 공통 작업 규칙 문서다. Codex와 Claude 모두 이 문서를 기본 기준으로 따른다.

## Project Context

- Stack: Spring Boot 4, Java 21, Gradle
- Formatting: `spotless` + `googleJavaFormat`
- Test framework: JUnit Platform, Spring Boot Test, Spring Security Test

## Working Rules

- 수정 전 관련 코드와 테스트를 먼저 읽는다.
- 동작 변경이 있으면 테스트를 추가하거나 기존 테스트를 갱신한다.
- 작업 완료 전 최소한 관련 테스트 또는 검증 명령을 직접 실행한다.
- 큰 구조 변경이나 요구사항이 모호한 작업은 바로 구현하지 말고 범위와 의도를 먼저 정리한다.

## Code Rules

- 와일드카드 import를 추가하지 않는다.
- 포맷팅은 `spotless` 기준을 따른다.
- 비밀값, 키, 토큰은 커밋하지 않는다.
- 보안 설정 변경 시 기존 인증/인가 흐름에 미치는 영향을 함께 점검한다.

## Domain-Driven Design

- 비즈니스 규칙은 우선 domain 레이어에 둔다.
- application 레이어는 유스케이스와 트랜잭션 경계를 담당한다.
- Repository는 aggregate root 기준으로 설계한다.
- 새로운 기능은 어떤 도메인과 aggregate에 속하는지 먼저 정리한다.

## Commands

- 개발 전 초기 설정: `git submodule init && git submodule update --recursive`
- 테스트: `./gradlew test`
- 포맷 검사: `./gradlew spotlessCheck`
- 포맷 적용: `./gradlew spotlessApply`
- 전체 검증: `./gradlew check`

## AI Workspace

- 재사용 스킬: `./.ai/skills/`
- 반복 커맨드 문서: `./.ai/commands/`
- spec/plan 기록: `./.ai/planning/`
- 템플릿: `./.ai/templates/`
