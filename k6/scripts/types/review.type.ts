import { CursorResponse } from './global.type.ts';

// ReviewResponse.java (author는 UserSummaryResponse 기준)
export interface ReviewResponse {
  id: string;
  contentId: string;
  author: {
    userId: string;
    name: string;
    profileImageUrl: string | null;
  };
  text: string;
  rating: number;
}

// 리뷰 목록 조회를 커서 페이지네이션 응답으로
export type ReviewListResponse = CursorResponse<ReviewResponse>;

// ReviewCreateRequest.java
export interface ReviewCreateRequest {
  contentId: string;
  text: string;
  rating: number;
}

// ReviewUpdateRequest.java
export interface ReviewUpdateRequest {
  text?: string;
  rating?: number;
}
