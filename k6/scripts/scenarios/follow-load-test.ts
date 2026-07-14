// 팔로우 부하테스트 — POST/GET/DELETE /api/follows
// VU 계정들끼리 서로 팔로우 대상이 되어줌 (관리자용 유저 목록 조회 불필요).
// 생성 → 조회 → 취소를 한 사이클로 묶어 반복 실행해도 중복 제약에 안 걸리게 함.

// k6 run scripts/scenarios/follow-load-test.ts
// # VU 수 조정: -e TARGET_VUS=30

import { check } from 'k6';
import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios } from '../config.ts';
import { createFollow, getFollowedByMe, getFollowerCount, deleteFollow } from '../api/follow.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { randomThinkTime } from '../utils/random.ts';
import { setupAuthWithProfile } from '../utils/setup.ts';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';

export const options = {
  // CSRF 쿠키를 VU당 한 번만 발급받아 재사용하기 위함 (기본값은 반복마다 쿠키 저장소 초기화)
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
    ...endpointThresholds(config.tags.follow.followedByMe, ['p(95)<300'], 'load'),
    ...endpointThresholds(config.tags.follow.count, ['p(95)<300'], 'load'),
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
    throw new Error('[setup] 팔로우 테스트엔 최소 2개 계정이 필요합니다.');
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

// warmup / load 두 시나리오가 공유하는 실행 함수
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
    const status = getFollowedByMe(me.token, target.userId);
    check(status, { '팔로우 상태 조회됨': (s) => s !== null });

    const count = getFollowerCount(me.token, target.userId);
    check(count, { '팔로워 수 조회됨': (c) => c !== null && c >= 1 });

    // 다음 반복에서 같은 조합이 다시 팔로우될 수 있도록 정리 (중복 제약 회피)
    const cleaned = deleteFollow(me.token, followed.id);
    check(cleaned, { '정리용 팔로우 취소 성공': (d) => d === true });
  }

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'follow-load-test-summary.html');
}
