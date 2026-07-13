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
  const url = config.endpoints.review.detail.replace('{reviewId}', reviewId);
  return patch<ReviewResponse>(url, body, {
    token,
    tag: config.tags.review.update,
  });
}

export function deleteReview(reviewId: string, token: string): void {
  const url = config.endpoints.review.detail.replace('{reviewId}', reviewId);
  del(url, { token, tag: config.tags.review.delete });
}
