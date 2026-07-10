---
name: git-workflow
description: 이 저장소의 브랜치 전략과 커밋 메시지 규칙 가이드
disable-model-invocation: true
---

# Git Workflow

## 브랜치 전략

| 브랜치 | 용도 |
|------|------|
| `main` | 운영 배포 브랜치 |
| `dev` | 개발 통합 브랜치 |
| `feat/xxx` | 기능 개발 |
| `fix/xxx` | 버그 수정 |
| `refactor/xxx` | 리팩토링 |
| `docs/xxx` | 문서 작업 |
| `chore/xxx` | 빌드, 설정, 의존성 변경 |
| `test/xxx` | 테스트 작업 |

## 브랜치 규칙

- `main`, `dev` 브랜치에 직접 푸시하지 않는다.
- 작업 브랜치는 항상 `dev`에서 분기한다.
- PR 머지 전 최소 1명 이상 승인을 받는다.

## 브랜치 예시

```bash
feat/SOMA-12-user-auth
fix/SOMA-21-login-error
refactor/SOMA-33-user-service
docs/SOMA-9-ai-rule-setup
```

## 브랜치 네이밍 규칙

- 형식은 `{type}/{issue-key}-{short-description}` 를 기본으로 한다.
- 가능하면 이슈 키를 포함한다.
- 설명은 짧고 의미가 드러나게 작성한다.

## 커밋 메시지 규칙

- 커밋 메시지는 한글로 작성한다.
- 형식은 `<type>: <내용>`을 따른다.
- 한 커밋에는 한 가지 의도만 담는다.

## Commit Type

| type | 용도 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변화 없는 코드 개선 |
| `test` | 테스트 추가/수정 |
| `docs` | 문서 수정 |
| `chore` | 빌드, 설정, 의존성 변경 |

## 커밋 예시

```bash
feat: 회원가입 API 추가
fix: 로그인 토큰 만료 오류 수정
refactor: UserService 메서드 분리
test: User 단위 테스트 추가
```

## 작업 전후 체크

### 작업 시작 전
- 현재 브랜치가 `dev` 또는 작업 브랜치인지 확인
- `main`에서 분기하지 않았는지 확인

### 커밋 전
- 불필요한 파일이 포함되지 않았는지 확인
- 포맷팅이 맞는지 확인
- 관련 테스트를 실행했는지 확인

### PR 전
- 대상 브랜치가 `dev`인지 확인
- 변경 이유와 영향 범위를 설명할 수 있어야 한다

## 주의사항

- Google Java Format 자동 적용 결과를 되돌리지 않는다.
- unrelated change를 섞어서 커밋하지 않는다.
