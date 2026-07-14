// 대화방/메시지 조회만 테스트 — GET /api/conversations, GET /api/conversations/{id}/direct-messages
// setup()에서 대화방을 미리 만들어두고, run()은 조회만 반복함.

// k6 run scripts/scenarios/dm-read-test.ts
// # VU 수 조정: -e TARGET_VUS=30

import { check } from 'k6';
import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios, SortDirection } from '../config.ts';
import { createOrGetConversation, getConversations, getMessages } from '../api/dm.api.ts';
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
    ...endpointThresholds(config.tags.conversation.list, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.directMessage.list, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = { token: string; userId: string; conversationId: string }[];

export function setup(): SetupData {
  const accounts = setupAuthWithProfile(TARGET_VUS);
  if (accounts.length < 2) {
    throw new Error('[setup] 최소 2개 계정이 필요합니다 (TARGET_VUS>=2).');
  }
  return accounts.map((account, i) => {
    const partner = accounts[(i + 1) % accounts.length];
    const conversation = createOrGetConversation(account.token, partner.userId);
    if (!conversation) {
      throw new Error(`[setup] 대화방 생성 실패 (account index=${i})`);
    }
    return { ...account, conversationId: conversation.id };
  });
}

export function run(data: SetupData): void {
  const account = data[(exec.vu.idInTest - 1) % data.length];

  const list = getConversations(account.token, { limit: 20, sortDirection: SortDirection.DESC });
  check(list, { '대화방 목록 조회됨': (l) => l !== null && Array.isArray(l.data) });

  const messages = getMessages(account.token, account.conversationId, 20, SortDirection.DESC);
  check(messages, { '메시지 목록 조회됨': (m) => m !== null && Array.isArray(m.data) });

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'dm-read-test-summary.html');
}
