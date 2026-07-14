// 리뷰 조회 부하테스트 (Read-heavy)
//   - 다수의 VU가 랜덤 콘텐츠의 리뷰 목록을 지속적으로 조회
//   - 쓰기 없음 → 순수 읽기 처리량·응답 시간 측정에 집중
//
//   실행 예시:
//     k6 run scripts/scenarios/review-read-test.ts
//     k6 run -e VUS=20 -e DURATION=2m -e CONTENT_POOL=100 scripts/scenarios/review-read-test.ts

import { check } from 'k6';
import exec from 'k6/execution';
import config, { commonThresholds, ContentSortBy, SortDirection } from '../config.ts';
import { get } from '../utils/http-client.ts';
import { randomThinkTime, pickOne } from '../utils/random.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { setupAuth } from '../utils/setup.ts';
import { getReviews } from '../api/review.api.ts';
import { CursorResponse } from '../types/global.type.ts';
import { ContentResponse } from '../types/content.type.ts';

const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '1m';
const CONTENT_POOL = Number(__ENV.CONTENT_POOL || 50);

export const options = {
  setupTimeout: '180s',
  scenarios: {
    review_read: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    ...commonThresholds,
    // 쓰기 없는 조회 전용이므로 CRUD 혼합 시나리오(500ms)보다 기준을 타이트하게
    'http_req_duration{name:GET /api/reviews}': ['p(95)<300'],
  },
};

interface SetupData {
  tokens: string[];
  contentIds: string[];
}

export function setup(): SetupData {
  const tokens = setupAuth(VUS);

  const contentIds: string[] = [];
  let nextCursor: string | null = null;
  let nextIdAfter: string | null = null;

  while (contentIds.length < CONTENT_POOL) {
    const params = [
      `limit=100`,
      `sortDirection=${SortDirection.DESC}`,
      `sortBy=${ContentSortBy.CREATED_AT}`,
      nextCursor ? `cursor=${encodeURIComponent(nextCursor)}` : '',
      nextIdAfter ? `idAfter=${nextIdAfter}` : '',
    ].filter(Boolean).join('&');

    const res = get<CursorResponse<ContentResponse>>(
      `${config.endpoints.content.list}?${params}`,
      { token: tokens[0] },
    );

    if (!res || res.data.length === 0) break;
    res.data.forEach((c) => contentIds.push(c.id));
    nextCursor = res.nextCursor;
    nextIdAfter = res.nextIdAfter;
    if (!res.hasNext) break;
  }

  if (contentIds.length === 0) {
    throw new Error('[setup] 콘텐츠가 없습니다. data-generator를 먼저 실행하세요.');
  }

  console.log(`[setup] 콘텐츠 ${contentIds.length}개 수집 완료`);
  return { tokens, contentIds };
}

export default function (data: SetupData): void {
  const token = data.tokens[(exec.vu.idInTest - 1) % data.tokens.length];
  const contentId = pickOne(data.contentIds);

  const listRes = getReviews(contentId, token);
  check(listRes, {
    '리뷰 목록 조회 성공': (r) => r !== null && Array.isArray(r?.data),
  });

  randomThinkTime(0.5, 2.0);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'summary-review-read.html');
}
