// DM 부하테스트 — 대화방 생성/조회, 메시지 목록 조회
// 메시지 "전송"은 STOMP 전용이라 범위 밖 (content 채팅과 동일). data-generator가 DM을
// 안 시딩해서 메시지 목록은 비어있는 게 정상.

// k6 run scripts/scenarios/dm-load-test.ts
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
    ...endpointThresholds(config.tags.conversation.create, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.conversation.list, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.directMessage.list, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = { token: string; userId: string }[];

export function setup(): SetupData {
  const accounts = setupAuthWithProfile(TARGET_VUS);
  if (accounts.length < 2) {
    throw new Error('[setup] DM 테스트엔 최소 2개 계정이 필요합니다 (TARGET_VUS>=2).');
  }
  return accounts;
}

// warmup / load 두 시나리오가 공유하는 실행 함수
export function run(data: SetupData): void {
  if (data.length < 2) {
    throw new Error('[VU] 대화 상대 풀이 부족합니다. setup()을 확인하세요.');
  }
  const myIndex = (exec.vu.idInTest - 1) % data.length;
  const me = data[myIndex];
  // 옆 인덱스 계정을 고정 대화상대로 삼아, VU마다 항상 같은 대화방을 재사용하게 한다
  const partner = data[(myIndex + 1) % data.length];

  const conversation = createOrGetConversation(me.token, partner.userId);
  check(conversation, {
    '대화방 응답 존재': (c) => c !== null,
    '상대방 일치': (c) => c?.with.userId === partner.userId,
  });

  const list = getConversations(me.token, { limit: 20, sortDirection: SortDirection.DESC });
  check(list, {
    '대화방 목록 조회됨': (l) => l !== null && Array.isArray(l.data),
  });

  if (conversation) {
    const messages = getMessages(me.token, conversation.id, 20, SortDirection.DESC);
    check(messages, {
      '메시지 목록 조회됨': (m) => m !== null && Array.isArray(m.data),
    });
  }

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'dm-load-test-summary.html');
}
