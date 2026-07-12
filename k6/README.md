# MOPL API 부하테스트 (k6)

MOPL API 부하테스트 **공용** 프로젝트입니다. 각 도메인 담당자가 이 뼈대 위에 자기 시나리오를 얹습니다.
TypeScript로 작성하며, **k6가 `.ts`를 직접 실행**하므로 webpack/docker 번들링이 필요 없습니다.

> 이 문서는 **모든 도메인 공통** 안내입니다. 콘텐츠 API 관련 내용은 맨 아래 "예시" 섹션에만
> 두었으니, 본인 도메인에 맞게 바꿔서 참고하세요.

## 사전 준비

1. **k6 설치** — `k6 version` 으로 확인
2. **대상 서버 실행** — 로컬 `8080` 포트 (예: `./gradlew bootRun`)
3. **테스트 데이터 시딩** — `data-generator` 실행 (도메인 데이터 + 로그인용 계정 생성)
   - 계정: `user1@loadtest.local` ~ `user{N}@loadtest.local`, 비밀번호 `Test1234!` (전부 고정)
   - 기존 데이터를 TRUNCATE 후 재생성하므로 **테스트 전 1회만** 돌리면 됨
4. (선택) **타입 지원** — 에디터 자동완성/타입검사를 받으려면:

   ```bash
   cd k6
   npm install
   ```

   > `@types/k6` 설치용이며, 실행 자체엔 필요 없습니다.

## 실행

로그인이 필요한 API(대부분)는 `setup()` 이 `VUS` 수만큼 계정(`user1..userN@loadtest.local`)을
미리 로그인합니다. data-generator 로 그만큼 유저가 시딩돼 있어야 합니다.

```bash
# 기본 (VUS=5, 비밀번호 Test1234!, BASE_URL=http://localhost:8080)
k6 run scripts/smoke.ts

# VU 수 조정
k6 run -e VUS=30 scripts/smoke.ts

# 비밀번호/BASE_URL 이 다르면 함께 주입
k6 run -e BASE_URL=http://localhost:8080 -e DATAGEN_PASSWORD=Test1234! -e VUS=10 scripts/smoke.ts
```

현재 `smoke.ts` 는 계정별 로그인 후 조회가 200으로 응답하는지 확인하는 **연결 스모크**만 수행합니다.

### HTML 리포트

테스트가 끝나면 터미널 요약과 함께 **`summary.html`** 파일이 실행 폴더에 생성됩니다
(`smoke.ts` 의 `handleSummary` → `utils/reporter.ts`). 브라우저로 열면 VU·RPS·에러율·응답시간·
엔드포인트별 지표를 볼 수 있어요.

- 시나리오에서 재사용: `export function handleSummary(data) { return summaryHandler(data); }`
- 엔드포인트별 표는 요청에 `tag(name)` 를 달아야 채워집니다 (공용 api 함수들은 이미 태그를 붙임).
- ⚠️ `reporter.ts` 는 원격 jslib(`textSummary`)를 import 하므로 **최초 실행 시 인터넷 연결**이 필요합니다.

## 인증 흐름

대부분의 API는 로그인이 필요하고(`authenticated()`), 일부는 추가로 `ADMIN` 역할이 필요합니다.

```text
1. GET  /api/auth/csrf-token   → Set-Cookie: XSRF-TOKEN=<값>
2. POST /api/auth/sign-in      (form-urlencoded: username=<email>&password=<pw>)
                                header: X-XSRF-TOKEN: <1번 쿠키 값>
                                → body(JSON): { accessToken, ... }
3. 이후 요청                    header: Authorization: Bearer <accessToken>
```

- `scripts/api/auth.api.ts` 의 `login(email, password)` 가 1~2번을 처리하고 accessToken을 반환합니다.
- `loginByIndex(i)` 는 `user{i}@loadtest.local` 계정으로 로그인하는 헬퍼입니다.
- ⚠️ **계정당 세션 1개** 정책이라 여러 VU가 같은 계정을 공유하면 서로 세션을 무효화(401)합니다.
  반드시 VU마다 별도 계정을 쓰세요 (`smoke.ts` 의 `exec.vu.idInTest` 매핑 참고).

### API별 인증 요구사항

| 구분 | 도메인 / 엔드포인트 |
| --- | --- |
| 🔴 ADMIN | User 관리(`GET /api/users`, role/locked 변경), Content 쓰기, 콘텐츠 수집(`/api/admin/**`) |
| 🟡 로그인 | Content 조회, Review, Follow, Notification, SSE, DM/Conversation, Watching(유저별) |
| 🟢 공개 | Auth, 회원가입(`POST /api/users`), **Playlist 전체**, Watching(콘텐츠별) |

> 근거는 `SecurityConfig`. 🟢 중 Playlist / 콘텐츠별 Watching 은 명시 규칙 없이 `anyRequest().permitAll()`
> 로 떨어져 열린 것으로 보이므로(인증 규칙 누락 가능성), 본인 도메인이 여기 해당하면 팀에 한 번 확인하세요.

## 폴더 구조

```text
k6/
├─ package.json / tsconfig.json / .gitignore
├─ README.md
└─ scripts/
   ├─ config.ts            # [공용] BASE_URL, 엔드포인트, threshold, 계정
   ├─ smoke.ts             # [공용] 스모크 (연결 점검 + 공용 패턴 예시)
   ├─ utils/
   │  ├─ http-client.ts     # [공용] get/post/patch/del 래퍼
   │  ├─ random.ts          # [공용] think time, 랜덤 헬퍼
   │  └─ reporter.ts        # [공용] handleSummary용 HTML 리포트 (summaryHandler)
   ├─ api/
   │  ├─ auth.api.ts        # [공용] CSRF 발급 + 로그인 (login / loginByIndex)
   │  └─ <domain>.api.ts    # [각자] 도메인 엔드포인트 호출 함수
   ├─ types/
   │  ├─ auth.type.ts       # [공용] LoginResponse
   │  └─ <domain>.type.ts   # [각자] 도메인 DTO 매핑
   └─ scenarios/
      └─ <domain>-load-test.ts  # [각자] 부하 흐름·VU 설계
```

- **공용**: `config` / `utils` / `api/auth.api.ts` / `types/auth.type.ts` — 모두가 재사용
- **각자 작성**: 본인 도메인의 `api` / `types` / `scenarios`

## 주의: 로컬 모듈 import는 확장자 필수

k6는 Node처럼 확장자를 자동으로 붙여주지 않습니다. 로컬 파일을 import할 땐 **반드시 `.ts`를 명시**하세요.

```typescript
// ❌ 안 됨 — "moduleSpecifier가 로컬 디스크에 없다" 에러
import config from './config';

// ✅ 확장자 명시
import config from './config.ts';
```

## 레이어 규칙

| 레이어 | 역할 | 예 |
| --- | --- | --- |
| `api/*.api.ts` | **API 호출 한 번** (부품) | `getReviews()`, `createComment()` |
| `scenarios/*.ts` | **호출들의 순서·반복·부하** (흐름) | `read-load-test`, `write-load-test` |
| `utils/`, `types/`, `config.ts` | **공용 부품** | http 래퍼, 응답 타입, 설정 |

## 부하 단계 및 VU 가이드

항상 **Smoke → Load → Stress** 순서로 진행합니다.

| 단계 | VU | 계정 필요 수 | 목적 |
| --- | --- | --- | --- |
| Smoke | 1 | 1 | 스크립트·엔드포인트 정상 동작 확인 |
| Load | 10 ~ 30 | 최대 VU만큼 | 평상시 트래픽 재현, 성능 측정 |
| Stress | 30에서 시작해 무너질 때까지 점진 증가 (약 50~80 근처에서 힙/CPU 한계 예상) | 도달할 최대 VU만큼 | 한계점(무너지는 지점) 탐색 |

> **계정 준비**: 한 계정을 여러 VU가 동시에 쓰면 "계정당 세션 1개" 정책 때문에 서로 세션을
> 무효화시켜 401이 납니다. **VU 수만큼 별도 계정**을 준비하고, `setup()`에서 미리 로그인해
> VU별로 고정 매핑하세요. 전체를 한 번에 돌릴 계획이면 **최대 VU(≈ Stress 목표)만큼** 준비하면
> 모든 단계에서 재사용됩니다. (이 스펙 기준 50개 정도면 충분)

### 대상 서버 스펙 (VU 산정 근거)

부하 대상이 배포 환경(AWS)일 경우, 프리티어 학습용이라 매우 작습니다 (`infra/terraform` 기준).

| 구성 | 스펙 |
| --- | --- |
| 앱 서버 | EC2 `t3.micro` (2 vCPU 버스터블, RAM 1GB), 앱 인스턴스 1대 |
| JVM 힙 | 최대 350MB (`-Xmx350m`) — 1차 병목 지점 |
| DB | RDS `db.t3.micro` (2 vCPU 버스터블, RAM 1GB) |

- **힙 350MB / 앱 1대**가 병목이라 VU를 높게 잡을 필요가 없습니다.
- `t3.micro`는 **버스터블(CPU 크레딧)**이라, 짧은 스파이크와 지속 부하의 결과가 다르게 나올 수 있습니다.
- **로컬(`localhost`)에 테스트하는 경우 위 표는 참고만** 하고, 본인 PC 사양 기준으로 VU를 조정하세요.

### Threshold(통과 기준) 가이드

`config.ts` 의 `commonThresholds` 는 **공용 출발점**일 뿐, 우리 시스템 실측값이 아닙니다.

```ts
export const commonThresholds = {
  http_req_failed: ['rate<0.01'],     // 에러율 1% 미만 (건강한 서비스 통념)
  http_req_duration: ['p(95)<500'],   // 95%가 500ms 이내 (웹 API 응답성 통념)
};
```

진짜 기준은 ① 팀 SLO/SLA, ② 베이스라인 측정값(한번 돌려본 평상시 수치보다 약간 위), ③ 테스트 목적에서 정합니다.
**시나리오마다 override 하세요.**

```ts
// ① 그대로
thresholds: commonThresholds

// ② 일부만 완화/강화
thresholds: { ...commonThresholds, http_req_duration: ['p(95)<800'] }

// ③ 엔드포인트별 (← HTML 리포트의 엔드포인트 표도 이때 채워짐)
thresholds: {
  ...commonThresholds,
  'http_req_duration{name:GET /api/contents}': ['p(95)<400'],
}
```

단계별 권장 방향:

| 단계 | threshold 방향 |
| --- | --- |
| Smoke | 엄격하게 (에러 0 기대) — 정상 동작 확인이 목적 |
| Load | 지키고 싶은 목표치 (예: `p(95)<500`) — 통과/실패가 의미 있음 |
| Stress | 느슨하게 하거나 생략 — **무너지는 지점을 찾는 게 목적**이라 엄격히 걸면 당연히 실패로 뜸 |

## 주의

- 비밀정보(토큰/비밀번호)·실행 결과물은 커밋하지 않습니다. (`.gitignore` 처리)
- `BASE_URL` 등 환경값은 `-e` 옵션(`__ENV`)으로 주입합니다.

---

## 참고: 콘텐츠 API 예시

공용 뼈대의 첫 예시로 콘텐츠 조회를 사용했습니다. 본인 도메인 작성 시 참고용입니다.

`GET /api/contents` (커서 페이징) 유효 파라미터:

- `limit`: 1~100 (필수)
- `sortDirection`: `ASC` | `DESC` (필수)
- `sortBy`: `CREATED_AT` | `WATCHER_COUNT` | `RATE` (필수)
- `typeEqual`: `MOVIE` | `TV_SERIES` | `SPORT` (선택)
- `keywordLike`, `tagsIn`, `cursor`, `idAfter` (선택)

관련 파일: `types/content.type.ts`(응답 타입), `smoke.ts`(조회 스모크 예시).
