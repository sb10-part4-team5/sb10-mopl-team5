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
    follow: {
      create: `${API}/follows`,
      followedByMe: `${API}/follows/followed-by-me`,
      count: `${API}/follows/count`,
      detail: `${API}/follows/{followId}`,
    },
    conversation: {
      list: `${API}/conversations`, // GET 목록 조회 / POST 생성-또는-조회 동일 경로
      with: `${API}/conversations/with`,
      detail: `${API}/conversations/{conversationId}`,
    },
    // 메시지 "전송"은 STOMP(/pub/conversations/{id}/direct-messages)로만 되어 있어 chat과 동일하게
    // http-client로 호출 불가. 여기 있는 건 REST로 되는 목록 조회·읽음 처리뿐.
    directMessage: {
      list: `${API}/conversations/{conversationId}/direct-messages`,
      read: `${API}/conversations/{conversationId}/direct-messages/{directMessageId}/read`,
    },
    user: {
      list: `${API}/users`,
      detail: `${API}/users/{userId}`, // GET 조회 / PATCH 프로필 수정(multipart) 동일 경로
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
      byUser: 'GET /api/users/{watcherId}/watching-sessions',
      byContent: 'GET /api/contents/{contentId}/watching-sessions',
    },
    chat: {
      pubChat: 'STOMP /pub/contents/{id}/chat',
    },
    follow: {
      create: 'POST /api/follows',
      followedByMe: 'GET /api/follows/followed-by-me',
      count: 'GET /api/follows/count',
      delete: 'DELETE /api/follows/{id}',
    },
    conversation: {
      create: 'POST /api/conversations',
      list: 'GET /api/conversations',
      with: 'GET /api/conversations/with',
      detail: 'GET /api/conversations/{id}',
    },
    directMessage: {
      list: 'GET /api/conversations/{id}/direct-messages',
      read: 'POST /api/conversations/{id}/direct-messages/{msgId}/read',
    },
    user: {
      list: 'GET /api/users',
      firstPage: 'GET /api/users (first-page)',
      emailExisting: 'GET /api/users (email-like-existing)',
      detail: 'GET /api/users/{id}',
      update: 'PATCH /api/users/{id}',
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

// 엔드포인트(name 태그)별 threshold 세트.
export function endpointThresholds(
  tagName: string,
  durationThresholds: string[] = ['p(95)<500'],
  scenario?: string,
) {
  const sel = scenario ? `name:${tagName},scenario:${scenario}` : `name:${tagName}`;
  return {
    [`http_req_duration{${sel}}`]: durationThresholds,
    [`http_reqs{${sel}}`]: ['count>=0'],
    [`http_req_failed{${sel}}`]: ['rate>=0'],
  };
}

// 워밍업(constant-vus) → 본 측정(ramping-vus) 2단계 시나리오 세트.
// 그 뒤에 load 가 시작된다. 두 시나리오 모두 exec 로 지정한 같은 함수를 실행한다.
// threshold/리포트는 endpointThresholds(tag, thresholds, 'load') 로 스코프해 워밍업을 측정에서 제외할 것.
export function warmupLoadScenarios(opts: {
  exec: string;
  targetVus: number;
  rampTime?: string;
  holdTime?: string;
  warmupVus?: number;
  warmupTime?: string;
}) {
  const warmupTime = opts.warmupTime ?? '20s';
  return {
    warmup: {
      executor: 'constant-vus',
      vus: opts.warmupVus ?? 5,
      duration: warmupTime,
      exec: opts.exec,
      startTime: '0s',
    },
    load: {
      executor: 'ramping-vus',
      startTime: warmupTime,
      startVUs: 0,
      stages: [
        { duration: opts.rampTime ?? '30s', target: opts.targetVus },
        { duration: opts.holdTime ?? '1m', target: opts.targetVus },
        { duration: opts.rampTime ?? '30s', target: 0 },
      ],
      exec: opts.exec,
    },
  };
}

export default config;
