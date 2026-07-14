// 대화방 생성만 테스트 — POST /api/conversations

// k6 run scripts/scenarios/dm-create-test.ts
// # VU 수 조정: -e TARGET_VUS=30

import { check } from 'k6';
import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios } from '../config.ts';
import { createOrGetConversation } from '../api/dm.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { randomThinkTime, randomInt } from '../utils/random.ts';
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
    ...endpointThresholds(config.tags.conversation.create, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = { token: string; userId: string }[];

export function setup(): SetupData {
  const accounts = setupAuthWithProfile(TARGET_VUS);
  if (accounts.length < 2) {
    throw new Error('[setup] 최소 2개 계정이 필요합니다 (TARGET_VUS>=2).');
  }
  return accounts;
}

// 상대를 고정하면 최초 1회만 생성되고 그 뒤로는 계속 같은 대화방을 조회하게 돼
// "생성" 테스트가 사실상 "조회" 테스트로 바뀐다. 반복마다 상대를 무작위로 바꿔
// 새로운 대화방 생성 경로를 계속 타도록 한다.
export function run(data: SetupData): void {
  const myIndex = (exec.vu.idInTest - 1) % data.length;
  const me = data[myIndex];

  let partnerIndex = randomInt(0, data.length - 2);
  if (partnerIndex >= myIndex) partnerIndex += 1;
  const partner = data[partnerIndex];

  const conversation = createOrGetConversation(me.token, partner.userId);
  check(conversation, {
    '대화방 응답 존재': (c) => c !== null,
    '상대방 일치': (c) => c?.with.userId === partner.userId,
  });

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'dm-create-test-summary.html');
}
