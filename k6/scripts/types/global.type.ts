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
