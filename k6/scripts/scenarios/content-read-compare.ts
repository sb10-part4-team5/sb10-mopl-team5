// 콘텐츠 조회 부하테스트 — 조회 조건별 비용 비교
//
// 목적: 정렬/검색 조건마다 응답 성능이 얼마나 다른지 "한 리포트에서 나란히" 본다.
//   콘텐츠 목록 쿼리는 정렬(createdAt/watcherCount/rate)에 인덱스가 없고,
//   keywordLike 는 LIKE '%..%' 라 인덱스를 못 쓰므로, 조건별 비용 차이가 클 것으로 예상된다.
//   → "무엇이 느린가"를 데이터로 확인하는 것이 이 시나리오의 핵심.
//
// 방식: 각 VU가 매 iteration마다 모든 케이스를 "함께" 실행한다 (커넥션 풀을 공유하는
//   실제 운영과 유사한 혼합 부하). 케이스별 요청 수가 동일해져 p95 비교도 공정하다.
//
// ⚠️ 이 방식은 케이스들이 서로 커넥션 풀 등을 다투므로, "인덱스 전/후처럼 특정 쿼리
//   하나의 순수 비용 변화"를 보려면 케이스 간 간섭이 없는 content-read-baseline.ts
//   (-e CASE=) 를 대신 쓸 것.

import { check } from 'k6';
import exec from 'k6/execution';
import config, { commonThresholds, endpointThresholds } from '../config.ts';
import { getContents } from '../api/content.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { randomThinkTime } from '../utils/random.ts';
import { CONTENT_QUERY_CASE_NAMES, CONTENT_QUERY_TAGS, buildCaseParams } from './content-query-cases.ts';
import { setupAuth } from '../utils/setup.ts';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';

// 검색어/태그는 임의 값이어도 쿼리 비용(LIKE 풀스캔, 서브쿼리)은 거의 동일하다.
// 실제 매칭되는 값으로 결과셋 크기까지 재보려면 -e KEYWORD=... -e TAG_NAME=... 로 주입.
const KEYWORD = __ENV.KEYWORD || 'a';
const TAG_NAME = __ENV.TAG_NAME || 'action';

export const options = {
  scenarios: {
    content_read_compare: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP_TIME, target: TARGET_VUS },
        { duration: HOLD_TIME, target: TARGET_VUS },
        { duration: RAMP_TIME, target: 0 },
      ],
    },
  },
  // 비교가 목적이라 개별 케이스는 pass/fail 을 강제하지 않는다(항상 통과하는 p(95)>=0 로 등록만).
  // 대신 리포트 표에서 케이스별 p95 를 눈으로 비교한다. 에러율만 느슨하게 감시.
  thresholds: {
    http_req_failed: ['rate<0.05'],
    ...CONTENT_QUERY_CASE_NAMES.reduce(
      (acc, c) => ({ ...acc, ...endpointThresholds(CONTENT_QUERY_TAGS[c], ['p(95)>=0']) }),
      {},
    ),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)>=0']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)>=0']),
  },
};

type SetupData = string[];

export function setup(): SetupData {
  return setupAuth(TARGET_VUS);
}

export default function (data: SetupData): void {
  if (data.length === 0) {
    throw new Error('[VU] 사용 가능한 계정 토큰이 없습니다.');
  }
  const token = data[(exec.vu.idInTest - 1) % data.length];

  // 모든 케이스를 순서대로 실행 (케이스별 동일 샘플 수 확보)
  for (const caseName of CONTENT_QUERY_CASE_NAMES) {
    const tag = CONTENT_QUERY_TAGS[caseName];
    const params = buildCaseParams(caseName, { keyword: KEYWORD, tagName: TAG_NAME });
    const body = getContents(token, params, tag);
    check(body, { [`${tag} OK`]: (b) => b !== null && Array.isArray(b?.data) });
    randomThinkTime(0.5, 1.5);
  }
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'content-read-compare-summary.html');
}
