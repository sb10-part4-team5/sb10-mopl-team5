// 알림 부하테스트 — GET /api/notifications, DELETE /api/notifications/{id}
// 각 VU는 자신의 알림 목록을 조회하고, 읽지 않은 알림이 있으면 첫 번째를 읽음 처리한다.
// data-generator가 시딩한 FOLLOWED/PLAYLIST_SUBSCRIBED 등의 알림이 테스트 대상이다.

// k6 run scripts/scenarios/notification-load-test.ts
// # VU 수 조정: -e TARGET_VUS=30

import { check } from 'k6';
import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios } from '../config.ts';
import { getNotifications, markNotificationRead } from '../api/notification.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { randomThinkTime } from '../utils/random.ts';
import { setupAuth } from '../utils/setup.ts';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';

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
    http_req_failed: ['rate<0.01'],
    ...endpointThresholds(config.tags.notification.list, ['p(95)<300'], 'load'),
    ...endpointThresholds(config.tags.notification.markRead, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = string[];

export function setup(): SetupData {
  return setupAuth(TARGET_VUS + WARMUP_VUS);
}

export function run(data: SetupData): void {
  const token = data[(exec.vu.idInTest - 1) % data.length];

  const page = getNotifications(token, { limit: 20 });
  check(page, {
    '알림 목록 조회 성공': (p) => p !== null,
    '알림 데이터 배열 존재': (p) => Array.isArray(p?.data),
  });

  // 읽지 않은 알림(= 아직 삭제되지 않은 알림) 중 첫 번째를 읽음 처리
  const first = page?.data?.[0];
  if (first) {
    const ok = markNotificationRead(token, first.id);
    check(ok, { '알림 읽음 처리 성공': (v) => v === true });
  }

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'notification-load-test-summary.html');
}
