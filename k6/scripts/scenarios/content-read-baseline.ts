// 콘텐츠 조회 부하테스트 — 단일 조건 격리 실행 (+ 워밍업)
//
// content-read-compare.ts 와 반대로, 한 번에 "케이스 하나만" 돌린다.
// 다른 쿼리 타입과 커넥션 풀을 다투지 않으므로, 인덱스 추가 전/후처럼
// "이 쿼리 자체의 비용 변화"를 순수하게 비교할 때 쓴다.
//
// -e CASE= 로 케이스 선택 (latest/popular/rating/keyword/tag, 기본 latest).
//   k6 run -e CASE=latest scripts/scenarios/content-read-baseline.ts

import { check } from 'k6';
import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios } from '../config.ts';
import { getContents } from '../api/content.api.ts';
import { loginByIndex } from '../api/auth.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { randomThinkTime } from '../utils/random.ts';
import {
  CONTENT_QUERY_CASE_NAMES,
  CONTENT_QUERY_TAGS,
  buildCaseParams,
  ContentQueryCase,
} from './content-query-cases.ts';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5); // TARGET_VUS 이하로 (계정 매핑 유지)
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';
const KEYWORD = __ENV.KEYWORD || 'a';
const TAG_NAME = __ENV.TAG_NAME || 'action';

const CASE = (__ENV.CASE || 'latest') as ContentQueryCase;
if (!CONTENT_QUERY_CASE_NAMES.includes(CASE)) {
  throw new Error(
    `[config] 알 수 없는 CASE="${CASE}". 가능한 값: ${CONTENT_QUERY_CASE_NAMES.join(', ')}`,
  );
}

const TAG = CONTENT_QUERY_TAGS[CASE];
const PARAMS = buildCaseParams(CASE, { keyword: KEYWORD, tagName: TAG_NAME });

export const options = {
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
    ...endpointThresholds(TAG, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = { token: string }[];

export function setup(): SetupData {
  const tokens: SetupData = [];
  for (let i = 1; i <= TARGET_VUS; i++) {
    tokens.push({ token: loginByIndex(i) });
  }
  if (tokens.length === 0) {
    throw new Error(`[setup] 로그인된 계정이 없습니다 (TARGET_VUS=${TARGET_VUS}).`);
  }
  console.log(`[setup] ${tokens.length}개 계정 로그인 완료 (CASE=${CASE})`);
  return tokens;
}

// warmup / load 두 시나리오가 공유하는 실행 함수
export function run(data: SetupData): void {
  if (data.length === 0) {
    throw new Error('[VU] 사용 가능한 계정 토큰이 없습니다. setup() 로그인 실패 여부를 확인하세요.');
  }
  const account = data[(exec.vu.idInTest - 1) % data.length];

  const body = getContents(account.token, PARAMS, TAG);

  check(body, {
    '응답 본문 존재': (b) => b !== null,
    'data 배열 존재': (b) => Array.isArray(b?.data),
  });

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, `content-read-${CASE}-summary.html`);
}
