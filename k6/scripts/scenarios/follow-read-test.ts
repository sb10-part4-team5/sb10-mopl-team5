// 팔로우 조회만 테스트 — GET /api/follows/followed-by-me, GET /api/follows/count
// setup()에서 각 계정이 옆 계정을 한 번씩 팔로우해두고, run()은 그 상태를 반복 조회만 함.

// k6 run scripts/scenarios/follow-read-test.ts
// # VU 수 조정: -e TARGET_VUS=30

import { check } from 'k6';
import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios } from '../config.ts';
import { createFollow, getFollowedByMe, getFollowerCount } from '../api/follow.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { randomThinkTime } from '../utils/random.ts';
import { setupAuthWithProfile } from '../utils/setup.ts';

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
    ...endpointThresholds(config.tags.follow.followedByMe, ['p(95)<300'], 'load'),
    ...endpointThresholds(config.tags.follow.count, ['p(95)<300'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = { token: string; userId: string; targetUserId: string }[];

// 각 계정이 옆 계정(index+1)을 미리 팔로우해서 조회할 대상을 만들어둔다
export function setup(): SetupData {
  const accounts = setupAuthWithProfile(TARGET_VUS);
  if (accounts.length < 2) {
    throw new Error('[setup] 최소 2개 계정이 필요합니다 (TARGET_VUS>=2).');
  }
  return accounts.map((account, i) => {
    const target = accounts[(i + 1) % accounts.length];
    const existing = getFollowedByMe(account.token, target.userId);
    if (!existing && !createFollow(account.token, target.userId)) {
      throw new Error(`[setup] 팔로우 관계 준비 실패 (account index=${i})`);
    }
    return { ...account, targetUserId: target.userId };
  });
}

export function run(data: SetupData): void {
  const account = data[(exec.vu.idInTest - 1) % data.length];

  const status = getFollowedByMe(account.token, account.targetUserId);
  check(status, { '팔로우 상태 조회됨': (s) => s !== null });

  const count = getFollowerCount(account.token, account.targetUserId);
  check(count, { '팔로워 수 조회됨': (c) => c !== null && c >= 1 });

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'follow-read-test-summary.html');
}
