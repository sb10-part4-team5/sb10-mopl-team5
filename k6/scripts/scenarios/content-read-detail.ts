// 콘텐츠 단건 조회 부하테스트 — GET /api/contents/{contentId}
//
// 유효한 contentId 가 필요하므로, setup()에서 목록을 페이지네이션하며 실제 ID 풀을 모아두고
// 각 VU가 그중 랜덤하게 조회한다 (랜덤 UUID 는 전부 404 라 부하 의미가 없음).
//
// 이 파일도 baseline.ts 처럼 "절대 수치"를 다른 값과 비교하는 용도라 워밍업을 둔다
// (warmup 으로 서버를 먼저 데운 뒤 load 만 측정. content-query-cases.ts 처럼 혼합하지 않고
// 단일 케이스만 반복하는 게 baseline/detail의 공통점).

// k6 run scripts/scenarios/content-read-detail.ts
// # 풀 크기 조정: -e ID_POOL=1000

import { check } from 'k6';
import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios, ContentSortBy, SortDirection } from '../config.ts';
import { getContents, getContent } from '../api/content.api.ts';
import { loginByIndex } from '../api/auth.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { randomThinkTime, pickOne } from '../utils/random.ts';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';
const ID_POOL = Number(__ENV.ID_POOL || 500); // 조회 대상 ID 풀 크기 (클수록 DB 버퍼 캐시 편중이 줄어 현실적)

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
    ...endpointThresholds(config.tags.content.detail, ['p(95)<500'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = {
  tokens: string[];      // 계정 토큰 (VU 인덱스로 매핑)
  contentIds: string[];  // 조회 대상 ID 풀
};

// 목록을 커서로 순회하며 contentId 를 target 개수만큼 모은다
function collectContentIds(token: string, target: number): string[] {
  const ids: string[] = [];
  let cursor: string | undefined;
  let idAfter: string | undefined;

  while (ids.length < target) {
    const page = getContents(token, {
      limit: 100,
      sortBy: ContentSortBy.CREATED_AT,
      sortDirection: SortDirection.DESC,
      cursor,
      idAfter,
    });
    if (!page || page.data.length === 0) break;

    for (const c of page.data) ids.push(c.id);
    if (!page.hasNext || !page.nextCursor || !page.nextIdAfter) break;

    cursor = page.nextCursor;
    idAfter = page.nextIdAfter;
  }
  return ids;
}

export function setup(): SetupData {
  const tokens: string[] = [];
  for (let i = 1; i <= TARGET_VUS; i++) {
    tokens.push(loginByIndex(i));
  }
  if (tokens.length === 0) {
    throw new Error(`[setup] 로그인된 계정이 없습니다 (TARGET_VUS=${TARGET_VUS}).`);
  }

  const contentIds = collectContentIds(tokens[0], ID_POOL);
  if (contentIds.length === 0) {
    throw new Error('[setup] 조회할 콘텐츠 ID를 확보하지 못했습니다. data-generator 로 콘텐츠를 시딩했는지 확인하세요.');
  }

  console.log(`[setup] 계정 ${tokens.length}개 로그인, 콘텐츠 ID ${contentIds.length}개 확보`);
  return { tokens, contentIds };
}

// warmup / load 두 시나리오가 공유하는 실행 함수
export function run(data: SetupData): void {
  if (data.tokens.length === 0 || data.contentIds.length === 0) {
    throw new Error('[VU] 토큰 또는 콘텐츠 ID 풀이 비어 있습니다. setup() 을 확인하세요.');
  }
  const token = data.tokens[(exec.vu.idInTest - 1) % data.tokens.length];
  const contentId = pickOne(data.contentIds);

  const body = getContent(token, contentId);

  check(body, {
    '응답 본문 존재': (b) => b !== null,
    '요청한 id와 일치': (b) => b?.id === contentId,
  });

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'content-read-detail-summary.html');
}
