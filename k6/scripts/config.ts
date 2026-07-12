// 부하테스트 공용 설정 — 엔드포인트 / 파라미터 / 임계값(threshold)
// BASE_URL 은 하드코딩하지 말고 실행 시 -e 로 주입: k6 run -e BASE_URL=http://localhost:8080 ...
//
// 엔드포인트는 도메인별로 그룹핑했습니다. 경로의 {xxx} 자리표시자는 각 api 함수에서
// 실제 값으로 치환해서 사용하세요. 예: config.endpoints.content.detail.replace('{contentId}', id)

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API = `${BASE_URL}/api`;
// STOMP/WebSocket 용 (http -> ws, https -> wss)
const WS_BASE = BASE_URL.replace(/^http/, 'ws');

export const config = {
  baseUrl: BASE_URL,
  wsBase: WS_BASE,

  endpoints: {
    // 로그인 / 인증
    auth: {
      csrfToken: `${API}/auth/csrf-token`, // GET, 로그인 전 CSRF 쿠키 발급
      signIn: `${API}/auth/sign-in`,       // POST (form-urlencoded: username/password)
    },

    // 콘텐츠
    content: {
      list: `${API}/contents`,             // GET(목록, 커서) / POST(생성, ADMIN)
      detail: `${API}/contents/{contentId}`, // GET(단건) / PATCH(ADMIN) / DELETE(ADMIN)
    },

    // 콘텐츠 리뷰 CRUD
    review: {
      list: `${API}/reviews`,              // GET(목록, 커서)
      create: `${API}/reviews`,            // POST(생성)
      detail: `${API}/reviews/{reviewId}`, // PATCH(수정) / DELETE(삭제)
    },

    // 플레이리스트 CRUD
    playlist: {
      list: `${API}/playlists`,               // GET(목록, 커서)
      create: `${API}/playlists`,             // POST(생성)
      detail: `${API}/playlists/{id}`,        // GET(단건) / PATCH(수정) / DELETE(삭제)
      content: `${API}/playlists/{playlistId}/contents/{contentId}`, // POST(추가) / DELETE(제거)
    },

    // 구독 CRUD (플레이리스트 구독)
    subscription: {
      // POST(구독) / DELETE(구독취소)
      toggle: `${API}/playlists/{playlistId}/subscription`,
    },

    // 시청 세션 (실시간 채팅의 REST 부분)
    watching: {
      byUser: `${API}/users/{watcherId}/watching-sessions`,    // GET
      byContent: `${API}/contents/{contentId}/watching-sessions`, // GET
    },

    // 콘텐츠 시청 실시간 채팅 (STOMP over WebSocket / SockJS)
    // ⚠️ 일반 HTTP 가 아니라 WebSocket+STOMP 라, utils/http-client.ts 로는 호출할 수 없습니다.
    //    부하테스트 시 k6 experimental websockets 모듈 + STOMP 프레이밍 + SockJS 경로가 필요합니다.
    chat: {
      wsEndpoint: `${WS_BASE}/ws`,             // SockJS 연결 엔드포인트
      pubChat: '/pub/contents/{id}/chat',      // 채팅 전송 (publish)
      subChat: '/sub/contents/{id}/chat',      // 채팅 수신 (subscribe)
      subWatch: '/sub/contents/{id}/watch',    // 시청자 상태 수신 (subscribe)
    },
  },

  // 지표 태그 (k6 태그로 엔드포인트별 응답시간/에러율을 분리해서 봄)
  tags: {
    auth: {
      csrfToken: 'GET /api/auth/csrf-token',
      signIn: 'POST /api/auth/sign-in',
    },
    content: {
      list: 'GET /api/contents',
      detail: 'GET /api/contents/{id}',
      create: 'POST /api/contents',
      update: 'PATCH /api/contents/{id}',
      delete: 'DELETE /api/contents/{id}',
    },
    review: {
      list: 'GET /api/reviews',
      create: 'POST /api/reviews',
      update: 'PATCH /api/reviews/{id}',
      delete: 'DELETE /api/reviews/{id}',
    },
    playlist: {
      list: 'GET /api/playlists',
      detail: 'GET /api/playlists/{id}',
      create: 'POST /api/playlists',
      update: 'PATCH /api/playlists/{id}',
      delete: 'DELETE /api/playlists/{id}',
      addContent: 'POST /api/playlists/{id}/contents/{contentId}',
      removeContent: 'DELETE /api/playlists/{id}/contents/{contentId}',
    },
    subscription: {
      subscribe: 'POST /api/playlists/{id}/subscription',
      unsubscribe: 'DELETE /api/playlists/{id}/subscription',
    },
    watching: {
      byUser: 'GET /api/users/{id}/watching-sessions',
      byContent: 'GET /api/contents/{id}/watching-sessions',
    },
    chat: {
      pubChat: 'STOMP /pub/contents/{id}/chat',
    },
  },

  // data-generator 로 시딩된 부하테스트 계정
  // 이메일: user1@loadtest.local ~ user{N}@loadtest.local (UserGenerator 순번 고정)
  // 비밀번호: Test1234! (data-generator 고정값, 필요 시 -e DATAGEN_PASSWORD 로 override)
  loadTestAccount: {
    password: __ENV.DATAGEN_PASSWORD || 'Test1234!',
    email(index: number): string {
      return `user${index}@loadtest.local`;
    },
  },
};

export const ContentSortBy = {
  CREATED_AT: 'CREATED_AT',
  WATCHER_COUNT: 'WATCHER_COUNT',
  RATE: 'RATE',
} as const;

export const SortDirection = {
  ASC: 'ASC',
  DESC: 'DESC',
} as const;

export const ContentTypeParam = {
  MOVIE: 'MOVIE',
  TV_SERIES: 'TV_SERIES',
  SPORT: 'SPORT',
} as const;

// 공통 통과 기준
export const commonThresholds = {
  http_req_failed: ['rate<0.01'],     // 에러율 1% 미만
  http_req_duration: ['p(95)<500'],   // 95%가 500ms 이내
};

export default config;
