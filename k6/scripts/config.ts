// 부하테스트 공용 설정. BASE_URL 은 -e 로 주입 (예: -e BASE_URL=http://localhost:8080).
// 경로의 {xxx} 는 각 api 함수에서 치환. 예: content.detail.replace('{contentId}', id)

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API = `${BASE_URL}/api`;
const WS_BASE = BASE_URL.replace(/^http/, 'ws'); // http->ws, https->wss

export const config = {
  baseUrl: BASE_URL,
  wsBase: WS_BASE,

  endpoints: {
    auth: {
      csrfToken: `${API}/auth/csrf-token`,
      signIn: `${API}/auth/sign-in`,
    },
    content: {
      list: `${API}/contents`,
      detail: `${API}/contents/{contentId}`,
    },
    review: {
      list: `${API}/reviews`,
      create: `${API}/reviews`,
      detail: `${API}/reviews/{reviewId}`,
    },
    playlist: {
      list: `${API}/playlists`,
      create: `${API}/playlists`,
      detail: `${API}/playlists/{id}`,
      content: `${API}/playlists/{playlistId}/contents/{contentId}`,
    },
    subscription: {
      toggle: `${API}/playlists/{playlistId}/subscription`, // POST 구독 / DELETE 취소
    },
    watching: {
      byUser: `${API}/users/{watcherId}/watching-sessions`,
      byContent: `${API}/contents/{contentId}/watching-sessions`,
    },
    // 실시간 채팅: STOMP over WebSocket(SockJS). HTTP 아님 → http-client 로 호출 불가.
    chat: {
      wsEndpoint: `${WS_BASE}/ws`,
      pubChat: '/pub/contents/{id}/chat',
      subChat: '/sub/contents/{id}/chat',
      subWatch: '/sub/contents/{id}/watch',
    },
  },

  // 엔드포인트별 지표 분리용 태그
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

  // data-generator 시딩 계정: user1@loadtest.local ~ (순번), 비번 Test1234! (-e DATAGEN_PASSWORD 로 override)
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
  http_req_duration: ['p(95)<500'],   // p95 500ms 이내
};

export default config;
