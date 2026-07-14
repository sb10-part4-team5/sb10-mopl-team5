// 팔로우 취소만 테스트 — DELETE /api/follows/{followId}
// 삭제할 대상이 있어야 하므로 매 반복 직전에 생성해둔다 (생성은 준비 단계, 측정 대상은 삭제만).

// k6 run scripts/scenarios/follow-delete-test.ts
// # VU 수 조정: -e TARGET_VUS=30

import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios } from '../config.ts';
import { createFollow, getFollowedByMe, deleteFollow } from '../api/follow.api.ts';
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
    ...endpointThresholds(config.tags.follow.delete, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = { token: string; userId: string }[];

// warmup+load 전체 VU 수만큼 계정을 만들고 id로 직접(나머지 연산 없이) 인덱싱한다.
// myIndex에 %를 걸면 두 시나리오의 VU id가 겹쳐 배분될 때 서로 다른 VU가 같은 인덱스를
// 쓰게 돼 팔로우 유니크 제약(409)에 걸린다.
// data-generator가 랜덤으로 팔로우를 미리 시딩해두므로, (i, i+1) 조합이 이미 존재할 수
// 있다. setup()에서 미리 지워서 run()이 항상 깨끗한 상태에서 시작하게 한다.
export function setup(): SetupData {
  const accounts = setupAuthWithProfile(TARGET_VUS + WARMUP_VUS);
  if (accounts.length < 2) {
    throw new Error('[setup] 최소 2개 계정이 필요합니다.');
  }
  accounts.forEach((account, i) => {
    const target = accounts[(i + 1) % accounts.length];
    const existing = getFollowedByMe(account.token, target.userId);
    if (existing) {
      deleteFollow(account.token, existing.id);
    }
  });
  return accounts;
}

export function run(data: SetupData): void {
  const myIndex = exec.vu.idInTest - 1;
  const me = data[myIndex];
  const target = data[(myIndex + 1) % data.length];

  const followed = createFollow(me.token, target.userId); // 준비 단계, 측정 대상 아님
  if (followed) {
    deleteFollow(me.token, followed.id); // 성공/실패는 http_req_failed 지표로 확인
  }

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'follow-delete-test-summary.html');
}
