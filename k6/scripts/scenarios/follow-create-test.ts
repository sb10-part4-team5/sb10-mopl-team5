// 팔로우 생성만 테스트 — POST /api/follows
// 매 반복 생성 직후 삭제해서 다음 반복에서 같은 쌍이 다시 생성 가능하게 함 (삭제는 정리용, 측정 대상 아님).

// k6 run scripts/scenarios/follow-create-test.ts
// # VU 수 조정: -e TARGET_VUS=30

import { check } from 'k6';
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
    ...endpointThresholds(config.tags.follow.create, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = { token: string; userId: string }[];

// k6는 warmup+load를 합쳐 VU id를 테스트 전체에서 겹치지 않게 부여하므로,
// 계정 풀도 동일하게 WARMUP_VUS+TARGET_VUS만큼 만들고 id로 직접(나머지 연산 없이) 인덱싱한다.
// 시나리오별로 풀을 나누거나 myIndex에 %를 걸면, 두 시나리오의 VU id가 뒤섞여 배분되면서
// 서로 다른 VU가 같은 인덱스로 겹치는 경우가 생겨 팔로우 유니크 제약(409)에 걸린다.
// data-generator가 유저마다 랜덤으로 10명씩 팔로우를 미리 시딩해두므로, 이 테스트가 쓸
// (i, i+1) 조합이 우연히 이미 존재할 수 있다. setup()에서 미리 확인 후 있으면 지워서
// run()이 항상 깨끗한 상태에서 시작하게 한다.
export function setup(): SetupData {
  const accounts = setupAuthWithProfile(TARGET_VUS + WARMUP_VUS);
  if (accounts.length < 2) {
    throw new Error('[setup] 최소 2개 계정이 필요합니다.');
  }
  accounts.forEach((account, i) => {
    const target = accounts[(i + 1) % accounts.length];
    const existing = getFollowedByMe(account.token, target.userId);
    if (existing && !deleteFollow(account.token, existing.id)) {
      throw new Error(`[setup] 기존 팔로우 관계 정리 실패 (account index=${i})`);
    }
  });
  return accounts;
}

export function run(data: SetupData): void {
  const myIndex = exec.vu.idInTest - 1;
  const me = data[myIndex];
  const target = data[(myIndex + 1) % data.length];

  const followed = createFollow(me.token, target.userId);
  check(followed, {
    '팔로우 응답 존재': (f) => f !== null,
    '팔로우 대상 일치': (f) => f?.followeeId === target.userId,
  });

  if (followed) {
    const cleaned = deleteFollow(me.token, followed.id); // 정리용, 측정 대상 아님
    check(cleaned, { '정리용 팔로우 취소 성공': (d) => d === true });
  }

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'follow-create-test-summary.html');
}
