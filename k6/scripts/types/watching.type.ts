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

// k6 에서는 전역 URLSearchParams가 기본적으로 없거나 버전/환경에 따라 ReferenceError가 날 수 있으므로 수동으로 조립합니다.
function buildQuery(params: WatchingSessionListParams): string {
  const pairs: string[] = [];
  pairs.push(`limit=${encodeURIComponent(String(params.limit ?? 10))}`);
  pairs.push(`sortBy=${encodeURIComponent(params.sortBy ?? "createdAt")}`);
  pairs.push(`sortDirection=${encodeURIComponent(params.sortDirection ?? "DESC")}`);
  if (params.watcherNameLike) pairs.push(`watcherNameLike=${encodeURIComponent(params.watcherNameLike)}`);
  if (params.cursor) pairs.push(`cursor=${encodeURIComponent(params.cursor)}`);
  if (params.idAfter) pairs.push(`idAfter=${encodeURIComponent(params.idAfter)}`);
  return pairs.join("&");
}
export { buildQuery };

