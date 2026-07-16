// SSE 부하테스트 — GET /api/sse
// 각 VU가 SSE 연결을 수립하고 HOLD_DURATION 동안 유지한다.
// SSE는 타임아웃으로 연결 시간을 제어하므로 http_req_failed 대신
// 커스텀 sse_connection_ok Rate 로 성공률을 측정한다.
// 연결 수립 지연시간은 http_req_waiting (TTFB) 기준.

// k6 run scripts/scenarios/sse-load-test.ts
// # 동시 연결 수 조정: -e TARGET_VUS=50
// # 연결 유지 시간 조정: -e HOLD_DURATION=10s

import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';
import config, { endpointThresholds, warmupLoadScenarios } from '../config.ts';
import { connectSse } from '../api/sse.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { setupAuth } from '../utils/setup.ts';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';
// VU당 SSE 연결 유지 시간. 늘릴수록 동시 연결 수가 올라가 서버 부하가 커진다.
const HOLD_DURATION = __ENV.HOLD_DURATION || '5s';

const sseConnectionOk = new Rate('sse_connection_ok');

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
    // 연결 성공률 99% 이상 (타임아웃은 성공으로 분류)
    sse_connection_ok: ['rate>0.99'],
    // 연결 수립 지연 (첫 이벤트 수신까지): p95 500ms 이내
    [`http_req_waiting{name:${config.tags.sse.subscribe},scenario:load}`]: ['p(95)<500'],
    ...endpointThresholds(config.tags.sse.subscribe, ['p(95)<500'], 'load'),
  },
};

type SetupData = string[];

export function setup(): SetupData {
  return setupAuth(TARGET_VUS + WARMUP_VUS);
}

export function run(data: SetupData): void {
  const token = data[(exec.vu.idInTest - 1) % data.length];

  const result = connectSse(token, HOLD_DURATION);

  sseConnectionOk.add(result.connected);
  check(result, {
    'SSE 연결 수립 성공': (r) => r.connected,
    '연결 수립 지연 500ms 이내': (r) => r.waitingMs < 500,
  });

  sleep(1);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'sse-load-test-summary.html');
}
