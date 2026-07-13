// ContentType.java 의 @JsonValue 값
export type ContentType = 'movie' | 'tvSeries' | 'sport';

// ContentResponse.java
export interface ContentResponse {
  id: string;
  type: ContentType;
  title: string;
  description: string;
  thumbnailUrl: string | null;
  tags: string[];
  averageRating: number;
  reviewCount: number;
  watcherCount: number;
}

// CursorResponse.java — 커서 기반 페이징 공통 응답
export interface CursorResponse<T> {
  data: T[];
  nextCursor: string | null;
  nextIdAfter: string | null;
  hasNext: boolean;
  totalCount: number;
  sortBy: string;
  sortDirection: string;
}
