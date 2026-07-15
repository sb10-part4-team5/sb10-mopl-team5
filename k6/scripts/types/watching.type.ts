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

// WatchingSessionCursorRequest.java 대응 (limit/sortDirection/sortBy 는 필수)
export interface WatchingSessionListParams {
  limit: number;
  sortDirection: string;
  sortBy: "createdAt"; // watching 세션은 createdAt만 정렬 가능
  watcherNameLike?: string;
  cursor?: string | null;
  idAfter?: string | null;
}

// k6 런타임엔 URLSearchParams 가 없어 직접 인코딩한다 (배열은 key=v1&key=v2 반복 — Spring List<String> 바인딩과 호환)
function buildQuery(params: WatchingSessionListParams): string {
  const parts: string[] = [
    `limit=${params.limit}`,
    `sortBy=${params.sortBy}`,
    `sortDirection=${params.sortDirection}`,
  ];
  if (params.watcherNameLike)
    parts.push(`watcherNameLike=${encodeURIComponent(params.watcherNameLike)}`);
  if (params.cursor) parts.push(`cursor=${encodeURIComponent(params.cursor)}`);
  if (params.idAfter)
    parts.push(`idAfter=${encodeURIComponent(params.idAfter)}`);
  return parts.join("&");
}

export { buildQuery };
