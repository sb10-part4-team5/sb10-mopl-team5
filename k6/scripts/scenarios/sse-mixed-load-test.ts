// SSE 복합 부하테스트 — SSE 연결 유지 VU + 알림 트리거 VU 동시 실행
// SSE_VUS 개 VU가 SSE 연결을 유지하는 동안, TRIGGER_VUS 개 VU가 팔로우를 생성/삭제해
// FOLLOWED 알림을 반복 발생시킨다. SSE 연결이 알림 발생 부하 하에서도 안정적으로
// 유지되는지, 연결 수립 지연이 증가하지 않는지 검증한다.

// k6 run scripts/scenarios/sse-mixed-load-test.ts
// # SSE 유지 VU 조정: -e SSE_VUS=20
// # 알림 트리거 VU 조정: -e TRIGGER_VUS=10

import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';
import config, { endpointThresholds } from '../config.ts';
import { connectSse } from '../api/sse.api.ts';
import { createFollow, deleteFollow, getFollowedByMe } from '../api/follow.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { setupAuthWithIds } from '../utils/setup.ts';
import { randomThinkTime } from '../utils/random.ts';

// trigger는 SSE ramp가 완료된 뒤 시작해야 활성 연결이 확보된 상태에서 알림을 발생시킬 수 있다
function addDurations(a: string, b: string): string {
  const toSec = (s: string) => (s.endsWith('m') ? parseFloat(s) * 60 : parseFloat(s));
  return `${toSec(a) + toSec(b)}s`;
}

const SSE_VUS = Number(__ENV.SSE_VUS || 15);
const TRIGGER_VUS = Number(__ENV.TRIGGER_VUS || 10);
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';
const HOLD_DURATION = __ENV.HOLD_DURATION || '5s';

// warmup과 SSE가 같은 계정 풀을 공유: warmup은 SSE 계정 앞 WARMUP_VUS개를 재사용
const SSE_POOL = Math.max(SSE_VUS, WARMUP_VUS);
// trigger 시작: warmup + ramp 이후 → SSE VU가 모두 연결된 상태에서 부하를 가한다
const TRIGGER_START_TIME = addDurations(WARMUP_TIME, RAMP_TIME);

const sseConnectionFailed = new Rate('sse_connection_failed');
const sseConnectEventReceived = new Rate('sse_connect_event_received');
const ssePrematureClose = new Rate('sse_premature_close');

interface SetupData {
  sseTokens: string[];
  sseUserIds: string[];
  triggerTokens: string[];
}

export const options = {
  noCookiesReset: true,
  scenarios: {
    warmup: {
      executor: 'constant-vus',
      vus: WARMUP_VUS,
      duration: WARMUP_TIME,
      exec: 'holdSse',
      startTime: '0s',
    },
    sse: {
      executor: 'ramping-vus',
      startTime: WARMUP_TIME,
      startVUs: 0,
      stages: [
        { duration: RAMP_TIME, target: SSE_VUS },
        { duration: HOLD_TIME, target: SSE_VUS },
        { duration: RAMP_TIME, target: 0 },
      ],
      exec: 'holdSse',
    },
    // SSE ramp 완료 후 알림 트리거 시작 → 활성 SSE 연결에 확실히 알림을 전달
    trigger: {
      executor: 'ramping-vus',
      startTime: TRIGGER_START_TIME,
      startVUs: 0,
      stages: [
        { duration: RAMP_TIME, target: TRIGGER_VUS },
        { duration: HOLD_TIME, target: TRIGGER_VUS },
        { duration: RAMP_TIME, target: 0 },
      ],
      exec: 'triggerNotification',
    },
  },
  thresholds: {
    // 복합 부하(팔로우 트리거 + SSE 동시 실행) 특성상 단일 테스트보다 완화된 기준 적용
    sse_connection_failed: ['rate<0.05'],
    sse_connect_event_received: ['rate>0.95'],
    // 복합 부하 특성상 단일 테스트보다 완화된 조기 종료율 허용
    sse_premature_close: ['rate<0.05'],
    [`http_req_waiting{name:${config.tags.sse.subscribe},scenario:sse}`]: ['p(95)<500'],
    [`http_req_duration{name:${config.tags.sse.subscribe},scenario:sse}`]: ['p(95)>=0'],
    [`http_reqs{name:${config.tags.sse.subscribe},scenario:sse}`]: ['count>=0'],
    ...endpointThresholds(config.tags.follow.create, ['p(95)<500'], 'trigger'),
    ...endpointThresholds(config.tags.follow.delete, ['p(95)<500'], 'trigger'),
  },
};

export function setup(): SetupData {
  const total = setupAuthWithIds(SSE_POOL + TRIGGER_VUS);
  return {
    sseTokens: total.tokens.slice(0, SSE_POOL),
    sseUserIds: total.userIds.slice(0, SSE_POOL),
    triggerTokens: total.tokens.slice(SSE_POOL),
  };
}

export function holdSse(data: SetupData): void {
  const idx = (exec.vu.idInTest - 1) % data.sseTokens.length;
  const token = data.sseTokens[idx];

  const result = connectSse(token, HOLD_DURATION);

  sseConnectionFailed.add(!result.connected);
  sseConnectEventReceived.add(result.hasConnectEvent);
  ssePrematureClose.add(result.connected && !result.timedOut);
  check(result, {
    'SSE 연결 수립 성공': (r) => r.connected,
    'connect 이벤트 수신': (r) => r.hasConnectEvent,
    '연결 수립 지연 500ms 이내': (r) => r.waitingMs < 500,
  });

  sleep(1);
}

// 팔로우 생성 후 즉시 삭제해 FOLLOWED 알림을 발생시킨다.
// 알림은 팔로우 생성 시점에 발행되므로 삭제해도 트리거에 영향 없고,
// 이터레이션 내 self-clean으로 잔여 팔로우가 누적되지 않는다.
export function triggerNotification(data: SetupData): void {
  const triggerIdx = (exec.vu.idInTest - 1) % data.triggerTokens.length;
  const token = data.triggerTokens[triggerIdx];

  // warmup-only 계정을 제외하고 SSE VU가 실제로 사용하는 계정만 알림 대상으로 삼는다
  const sseOnlyCount = Math.min(SSE_VUS, data.sseUserIds.length);
  const targetId = data.sseUserIds[Math.floor(Math.random() * sseOnlyCount)];
  let follow = createFollow(token, targetId);

  if (!follow) {
    // 이미 팔로우 중(409): 기존 팔로우를 삭제 후 재생성
    const existing = getFollowedByMe(token, targetId);
    if (existing) {
      deleteFollow(token, existing.id);
      sleep(1);
      follow = createFollow(token, targetId);
    }
  }

  check(follow, { '팔로우 생성 성공 (알림 트리거)': (r) => r !== null });

  if (follow) {
    sleep(1);
    deleteFollow(token, follow.id);
  }

  randomThinkTime(2, 5);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'sse-mixed-load-test-summary.html');
}
