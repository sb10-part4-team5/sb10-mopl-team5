// 리뷰 부하테스트
//   - 1 VU가 CONTENTS_PER_VU 개의 콘텐츠에 순서대로 리뷰 CRUD 수행
//   - VU끼리 콘텐츠 구간이 겹치지 않아 409(중복 리뷰) 충돌 없음
//
//   실행 예시:
//     k6 run scripts/scenarios/review-load-test.ts
//     k6 run -e VUS=10 -e CONTENTS_PER_VU=5 scripts/scenarios/review-load-test.ts

import { check } from 'k6';
import exec from 'k6/execution';
import config, { commonThresholds, ContentSortBy, SortDirection } from '../config.ts';
import { get } from '../utils/http-client.ts';
import { randomThinkTime, randomInt } from '../utils/random.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { fetchCsrfToken } from '../api/auth.api.ts';
import { setupAuth } from '../utils/setup.ts';

import { getReviews, createReview, updateReview, deleteReview } from '../api/review.api.ts';

import { CursorResponse } from '../types/global.type.ts';
import { ContentResponse } from '../types/content.type.ts';

const VUS = Number(__ENV.VUS || 5);
const CONTENTS_PER_VU = Number(__ENV.CONTENTS_PER_VU || 10);

export const options = {
  vus: VUS,
  iterations: VUS * CONTENTS_PER_VU,
  thresholds: {
    ...commonThresholds,
    'http_req_duration{name:GET /api/reviews}': ['p(95)<500'],
    'http_req_duration{name:POST /api/reviews}': ['p(95)<800'],
    'http_req_duration{name:PATCH /api/reviews/{id}}': ['p(95)<800'],
    'http_req_duration{name:DELETE /api/reviews/{id}}': ['p(95)<800'],
  },
};

interface SetupData {
  tokens: string[];
  contentIds: string[];
}

export function setup(): SetupData {
  const tokens = setupAuth(VUS);

  // 테스트에 필요한 콘텐츠 ID 수집 (VUS * CONTENTS_PER_VU 개)
  const contentIds: string[] = [];
  let nextCursor: string | null = null;
  let nextIdAfter: string | null = null;
  const needed = VUS * CONTENTS_PER_VU;

  while (contentIds.length < needed) {
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

    // 커서가 전진하지 않으면 중복 수집 방지
    if (res.nextCursor === nextCursor && res.nextIdAfter === nextIdAfter) {
      console.warn('[setup] 커서가 전진하지 않아 페이지네이션을 중단합니다.');
      break;
    }

    nextCursor = res.nextCursor;
    nextIdAfter = res.nextIdAfter;
    if (!res.hasNext) break;
  }

  if (contentIds.length < needed) {
    throw new Error(
      `[setup] 콘텐츠가 부족합니다. 필요: ${needed}, 실제: ${contentIds.length}. data-generator를 먼저 실행하세요.`,
    );
  }

  console.log(`[setup] 콘텐츠 ${contentIds.length}개 수집 완료`);
  return { tokens, contentIds };
}

export default function (data: SetupData): void {
  // 현재 VU 인덱스(0-based)와 반복 인덱스(0-based)로 담당 콘텐츠 결정
  const vuIndex = exec.vu.idInTest - 1;
  const iterIndex = exec.vu.iterationInScenario;
  const contentId = data.contentIds[vuIndex * CONTENTS_PER_VU + iterIndex];
  const token = data.tokens[vuIndex % data.tokens.length];

  // CSRF 쿠키 초기화 (http-client가 POST/PATCH/DELETE 직전에 jar에서 최신 값을 읽음)
  fetchCsrfToken();

  // 1. 리뷰 목록 조회
  const listRes = getReviews(contentId, token);
  check(listRes, {
    '리뷰 목록 조회 성공': (r) => r !== null && Array.isArray(r.data),
  });

  randomThinkTime(0.5, 1.5);

  // 2. 리뷰 생성
  const createRes = createReview(
    {
      contentId,
      text: `부하테스트 리뷰 - VU${exec.vu.idInTest} iter${iterIndex}`,
      rating: Number((randomInt(0, 10) / 2).toFixed(1)), // 0.0 ~ 5.0, 0.5 단위
    },
    token,
  );
  const created = check(createRes, {
    '리뷰 생성 성공': (r) => r !== null && typeof r?.id === 'string',
  });

  if (!created || !createRes) {
    return; // 생성 실패 시 이후 단계 스킵
  }

  randomThinkTime(0.5, 1.5);

  // 3. 리뷰 수정
  const updateRes = updateReview(
    createRes.id,
    { text: `수정된 리뷰 - VU${exec.vu.idInTest}`, rating: 4.0 },
    token,
  );
  check(updateRes, {
    '리뷰 수정 성공': (r) => r !== null && r?.text?.startsWith('수정된 리뷰'),
  });

  randomThinkTime(0.5, 1.5);

  // 4. 리뷰 삭제
  const deleteRes = deleteReview(createRes.id, token);
  check(deleteRes, {
    '리뷰 삭제 성공': (r) => r !== null && r.status === 204,
  });
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'summary-review.html');
}
