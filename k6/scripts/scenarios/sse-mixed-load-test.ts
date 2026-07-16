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

const SSE_VUS = Number(__ENV.SSE_VUS || 15);
const TRIGGER_VUS = Number(__ENV.TRIGGER_VUS || 10);
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';
const HOLD_DURATION = __ENV.HOLD_DURATION || '5s';

const sseConnectionFailed = new Rate('sse_connection_failed');
const sseConnectEventReceived = new Rate('sse_connect_event_received');

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
    // SSE 연결이 자리잡은 뒤 알림을 발생시켜 복합 부하를 가한다
    trigger: {
      executor: 'ramping-vus',
      startTime: WARMUP_TIME,
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
    [`http_req_waiting{name:${config.tags.sse.subscribe},scenario:sse}`]: ['p(95)<500'],
    [`http_req_duration{name:${config.tags.sse.subscribe},scenario:sse}`]: ['p(95)>=0'],
    [`http_reqs{name:${config.tags.sse.subscribe},scenario:sse}`]: ['count>=0'],
    ...endpointThresholds(config.tags.follow.create, ['p(95)<500'], 'trigger'),
    ...endpointThresholds(config.tags.follow.delete, ['p(95)<500'], 'trigger'),
  },
};

export function setup(): SetupData {
  const total = setupAuthWithIds(SSE_VUS + WARMUP_VUS + TRIGGER_VUS);
  const sseTokens = total.tokens.slice(0, SSE_VUS + WARMUP_VUS);
  const sseUserIds = total.userIds.slice(0, SSE_VUS + WARMUP_VUS);
  const triggerTokens = total.tokens.slice(SSE_VUS + WARMUP_VUS);

  console.log(
    `[setup] 전체=${total.tokens.length} sseTokens=${sseTokens.length} triggerTokens=${triggerTokens.length}`,
  );
  console.log(`[setup] sseTokens[0]=${sseTokens[0]?.slice(0, 20) ?? 'EMPTY'}`);
  console.log(`[setup] triggerTokens[0]=${triggerTokens[0]?.slice(0, 20) ?? 'EMPTY'}`);

  return { sseTokens, sseUserIds, triggerTokens };
}

export function holdSse(data: SetupData): void {
  const idx = (exec.vu.idInTest - 1) % data.sseTokens.length;
  const token = data.sseTokens[idx];

  const result = connectSse(token, HOLD_DURATION);

  sseConnectionFailed.add(!result.connected);
  sseConnectEventReceived.add(result.hasConnectEvent);
  check(result, {
    'SSE 연결 수립 성공': (r) => r.connected,
    'connect 이벤트 수신': (r) => r.hasConnectEvent,
    '연결 수립 지연 500ms 이내': (r) => r.waitingMs < 500,
  });

  sleep(1);
}

// 팔로우 생성/삭제를 반복해 SSE VU 계정으로 FOLLOWED 알림을 발생시킨다
let currentFollowId: string | null = null;

export function triggerNotification(data: SetupData): void {
  if (!data.triggerTokens?.length) {
    console.error(
      `[trigger] triggerTokens가 비어있음! sseTokens=${data.sseTokens?.length ?? 'undefined'}`,
    );
    return;
  }

  const triggerIdx = (exec.vu.idInTest - 1) % data.triggerTokens.length;
  const token = data.triggerTokens[triggerIdx];

  if (!token) {
    console.error(
      `[trigger] VU=${exec.vu.idInTest} triggerIdx=${triggerIdx} token이 없음! length=${data.triggerTokens.length}`,
    );
    return;
  }

  // 무작위로 타깃 SSE 유저를 선택해 모든 SSE VU에 부하를 고르게 분산
  const targetId = data.sseUserIds[Math.floor(Math.random() * data.sseUserIds.length)];

  if (currentFollowId) {
    deleteFollow(token, currentFollowId);
    currentFollowId = null;
    sleep(1);
  }

  let follow = createFollow(token, targetId);

  if (!follow) {
    // 이미 팔로우 중(409 등)인 경우: 기존 팔로우를 찾아 삭제 후 재생성
    const existing = getFollowedByMe(token, targetId);
    if (existing) {
      deleteFollow(token, existing.id);
      sleep(1);
      follow = createFollow(token, targetId);
    }
  }

  check(follow, { '팔로우 생성 성공 (알림 트리거)': (r) => r !== null });
  if (follow) {
    currentFollowId = follow.id;
  }

  randomThinkTime(2, 5);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'sse-mixed-load-test-summary.html');
}
