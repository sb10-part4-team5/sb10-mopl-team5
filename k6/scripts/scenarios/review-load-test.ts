// 리뷰 부하테스트
//    - 1 VU가 CONTENTS_PER_VU 개의 콘텐츠에 순서대로 리뷰 CRUD 수행
//    - 데이터 제네레이터가 생성한 리뷰를 제외하면, K6 실행 중에는 VU 간 리뷰 작성 충돌이 발생하지 않는다.
//    - 데이터 제네레이터가 미리 생성한 동일 사용자의 리뷰가 존재하면 새로 생성하지 않고 재사용한다.
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

import { getReviews, createReview, updateReview, deleteReview, findMyReview } from '../api/review.api.ts';

import { CursorResponse } from '../types/global.type.ts';
import { ContentResponse } from '../types/content.type.ts';

const VUS = Number(__ENV.VUS || 5);
const CONTENTS_PER_VU = Number(__ENV.CONTENTS_PER_VU || 10);

export const options = {
  setupTimeout: '180s',
  scenarios: {
    review_crud: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: CONTENTS_PER_VU, // VU마다 정확히 CONTENTS_PER_VU번 실행 보장
      maxDuration: '10m',
    },
  },
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
  let nextCursor: string | null = null; // 첫 페이지는 해당 정보들이 null
  let nextIdAfter: string | null = null;
  const needed = VUS * CONTENTS_PER_VU; // 리뷰가 겹치지 않도록 VUS * CPV 만큼의 콘텐츠를 가져와야 함

  while (contentIds.length < needed) {
    const params: string = [
      `limit=100`,
      `sortDirection=${SortDirection.DESC}`,
      `sortBy=${ContentSortBy.CREATED_AT}`,
      nextCursor ? `cursor=${encodeURIComponent(nextCursor)}` : '',
      nextIdAfter ? `idAfter=${encodeURIComponent(nextIdAfter)}` : '',
    ].filter(Boolean).join('&');

    const res: CursorResponse<ContentResponse> | null = get<CursorResponse<ContentResponse>>(
      `${config.endpoints.content.list}?${params}`,
      { token: tokens[0] },
    );

    if (!res || res.data.length === 0) break;
    res.data.forEach((c: ContentResponse) => contentIds.push(c.id));

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

  // 2. 리뷰 생성 (이전 실행에서 삭제되지 않은 리뷰가 남아 409가 날 수 있으므로 기존 리뷰 재사용)
  const myEmail = config.loadTestAccount.email(vuIndex + 1);
  const existingReview = listRes?.data.find((r) => r.author.email === myEmail);

  let reviewId: string;
  if (existingReview) {
    reviewId = existingReview.id;
  } else {
    const createRes = createReview(
      {
        contentId,
        text: `부하테스트 리뷰 - VU${exec.vu.idInTest} iter${iterIndex}`,
        rating: Number((randomInt(0, 10) / 2).toFixed(1)),
      },
      token,
    );
    const created = check(createRes, {
      '리뷰 생성 성공': (r) => r !== null && typeof r?.id === 'string',
    });

    if (!created || !createRes) {
      // 409 등으로 CREATE 실패 → 전체 목록 페이지네이션으로 기존 리뷰 탐색
      const found = findMyReview(contentId, myEmail, token);
      if (!found) return;
      reviewId = found.id;
    } else {
      reviewId = createRes.id;
    }
  }

  randomThinkTime(0.5, 1.5);

  // 3. 리뷰 수정
  const updateRes = updateReview(
    reviewId,
    { text: `수정된 리뷰 - VU${exec.vu.idInTest}`, rating: 4.0 },
    token,
  );
  check(updateRes, {
    '리뷰 수정 성공': (r) => r !== null && r?.text?.startsWith('수정된 리뷰'),
  });

  randomThinkTime(0.5, 1.5);

  // 4. 리뷰 삭제
  const deleteRes = deleteReview(reviewId, token);
  check(deleteRes, {
    '리뷰 삭제 성공': (r) => r !== null && r.status === 204,
  });
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'summary-review.html');
}
