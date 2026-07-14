import { RefinedResponse, ResponseType } from 'k6/http';
import { get, post, patch, del } from '../utils/http-client.ts';
import config from '../config.ts';
import { ReviewListResponse, ReviewResponse, ReviewCreateRequest, ReviewUpdateRequest } from '../types/review.type.ts';

export function getReviews(contentId: string, token: string): ReviewListResponse | null {
  const url = `${config.endpoints.review.list}?contentId=${contentId}&limit=10&sortDirection=DESC&sortBy=CREATED_AT`;
  return get<ReviewListResponse>(url, {
    token,
    tag: config.tags.review.list,
  });
}

export function createReview(body: ReviewCreateRequest, token: string): ReviewResponse | null {
  return post<ReviewResponse>(config.endpoints.review.create, body, {
    token,
    tag: config.tags.review.create,
  });
}

export function updateReview(reviewId: string, body: ReviewUpdateRequest, token: string): ReviewResponse | null {
  const url = config.endpoints.review.detail.split('{reviewId}').join(reviewId);
  return patch<ReviewResponse>(url, body, {
    token,
    tag: config.tags.review.update,
  });
}

export function deleteReview(reviewId: string, token: string): RefinedResponse<ResponseType | undefined> {
  const url = config.endpoints.review.detail.split('{reviewId}').join(reviewId);
  return del(url, { token, tag: config.tags.review.delete });
}

// 커서 페이지네이션으로 전체 목록을 순회하며 특정 이메일의 리뷰를 찾는다
export function findMyReview(contentId: string, userEmail: string, token: string): ReviewResponse | null {
  let cursor: string | null = null;
  let idAfter: string | null = null;

  while (true) {
    const params = [`contentId=${contentId}`, `limit=100`, `sortDirection=DESC`, `sortBy=CREATED_AT`];
    if (cursor) params.push(`cursor=${encodeURIComponent(cursor)}`);
    if (idAfter) params.push(`idAfter=${idAfter}`);

    const res = get<ReviewListResponse>(
      `${config.endpoints.review.list}?${params.join('&')}`,
      { token, tag: config.tags.review.list },
    );

    if (!res) return null;
    const found = res.data.find((r) => r.author.email === userEmail);
    if (found) return found;
    if (!res.hasNext || !res.nextCursor || !res.nextIdAfter) return null;

    cursor = res.nextCursor;
    idAfter = res.nextIdAfter;
  }
}
