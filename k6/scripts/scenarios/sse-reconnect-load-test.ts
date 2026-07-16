// SSE 재연결 부하테스트 — Last-Event-ID 헤더를 이용한 재연결 시나리오
// 각 VU가 자신의 미읽음 알림 ID를 Last-Event-ID로 사용해 재연결하고,
// 서버가 missed notification을 포함한 응답을 보내는지 검증한다.
// (SseService.sendMissedEvents → NotificationService.findMissedNotifications 경로 커버)

// k6 run scripts/scenarios/sse-reconnect-load-test.ts
// # 동시 연결 수 조정: -e TARGET_VUS=20
// # 연결 유지 시간 조정: -e HOLD_DURATION=5s

import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';
import config, { warmupLoadScenarios } from '../config.ts';
import { connectSse } from '../api/sse.api.ts';
import { getNotifications } from '../api/notification.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { setupAuth } from '../utils/setup.ts';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';
const HOLD_DURATION = __ENV.HOLD_DURATION || '5s';

const sseConnectionFailed = new Rate('sse_connection_failed');
const sseConnectEventReceived = new Rate('sse_connect_event_received');
const sseMissedNotificationReceived = new Rate('sse_missed_notification_received');
const ssePrematureClose = new Rate('sse_premature_close');

interface SetupData {
  tokens: string[];
  lastEventIds: string[];
}

export const options = {
  noCookiesReset: true,
  scenarios: warmupLoadScenarios({
    exec: 'run',
    targetVus: TARGET_VUS,
    rampTime: RAMP_TIME,
    holdTime: HOLD_TIME,
    warmupVus: WARMUP_VUS,
    warmupTime: WARMUP_TIME,
  }),
  thresholds: {
    sse_connection_failed: ['rate<0.01'],
    sse_connect_event_received: ['rate>0.99'],
    // Last-Event-ID 재연결 핵심 지표: missed notification이 실제로 전달되어야 한다
    sse_missed_notification_received: ['rate>0.99'],
    sse_premature_close: ['rate<0.01'],
    [`http_req_waiting{name:${config.tags.sse.subscribe},scenario:load}`]: ['p(95)<500'],
    [`http_req_duration{name:${config.tags.sse.subscribe},scenario:load}`]: ['p(95)>=0'],
    [`http_reqs{name:${config.tags.sse.subscribe},scenario:load}`]: ['count>=0'],
  },
};

export function setup(): SetupData {
  const tokens = setupAuth(TARGET_VUS + WARMUP_VUS);

  // 오래된 알림 2건을 조회해 첫 번째를 Last-Event-ID로, 두 번째 이후를 missed notification으로 활용
  // 알림이 1건뿐인 계정은 재연결 후 받을 missed notification이 없으므로 제외한다
  const validTokens: string[] = [];
  const validLastEventIds: string[] = [];
  tokens.forEach((token) => {
    const page = getNotifications(token, { limit: 2, sortDirection: 'ASCENDING' });
    const items = page?.data ?? [];
    if (items.length >= 2) {
      validTokens.push(token);
      validLastEventIds.push(items[0].id);
    }
  });

  console.log(`[setup] Last-Event-ID 확보: ${validTokens.length}/${tokens.length}개 계정`);
  if (validTokens.length === 0) {
    throw new Error('Last-Event-ID를 확보할 수 있는 계정이 없어 테스트를 중단합니다.');
  }
  return { tokens: validTokens, lastEventIds: validLastEventIds };
}

export function run(data: SetupData): void {
  const idx = (exec.vu.idInTest - 1) % data.tokens.length;
  const token = data.tokens[idx];
  const lastEventId = data.lastEventIds[idx] ?? undefined;

  const result = connectSse(token, HOLD_DURATION, lastEventId);

  sseConnectionFailed.add(!result.connected);
  sseConnectEventReceived.add(result.hasConnectEvent);
  sseMissedNotificationReceived.add(result.missedNotificationCount > 0);
  ssePrematureClose.add(result.connected && !result.timedOut);
  check(result, {
    'SSE 재연결 성공': (r) => r.connected,
    'connect 이벤트 수신': (r) => r.hasConnectEvent,
    'missed notification 수신': (r) => r.missedNotificationCount > 0,
    '조기 종료 없음': (r) => !r.connected || r.timedOut,
    '연결 수립 지연 500ms 이내': (r) => r.waitingMs < 500,
  });

  sleep(1);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'sse-reconnect-load-test-summary.html');
}
