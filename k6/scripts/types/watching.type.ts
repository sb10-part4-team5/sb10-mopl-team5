// CursorResponse는 도메인 공통 페이징 응답이라 global.type.ts에 있는 걸 그대로 재사용
import { CursorResponse } from "./global.type.ts";

// WatchingSessionResponse.java — 사용자별 시청 세션 단건 조회 응답
export interface WatchingSessionResponse {
  id: string;
  createdAt: string;
  watcher: {
    userId: string;
    name: string | null;
    profileImageUrl: string | null;
  };
  contentId: string;
}

// 콘텐츠별 시청 세션 목록 조회 응답 (커서 페이지네이션)
export type WatchingSessionListResponse =
  CursorResponse<WatchingSessionResponse>;

// WatchingSessionCursorRequest.java 대응
// API에서 기본값을 제공하므로 모든 필드를 선택사항으로 정의
export interface WatchingSessionListParams {
  limit: number;
  sortDirection: string;
  sortBy: "createdAt"; // watching 세션은 createdAt만 정렬 가능
  watcherNameLike?: string;
  cursor?: string | null;
  idAfter?: string | null;
}

// k6 v0.40.0+부터 URLSearchParams API를 공식 지원하므로 표준 방식을 사용한다
function buildQuery(params: WatchingSessionListParams): string {
  const searchParams = new URLSearchParams();
  searchParams.set("limit", String(params.limit ?? 10));
  searchParams.set("sortBy", params.sortBy ?? "createdAt");
  searchParams.set("sortDirection", params.sortDirection ?? "DESC");
  if (params.watcherNameLike) searchParams.set("watcherNameLike", params.watcherNameLike);
  if (params.cursor) searchParams.set("cursor", params.cursor);
  if (params.idAfter) searchParams.set("idAfter", params.idAfter);
  return searchParams.toString();
}
export { buildQuery };

