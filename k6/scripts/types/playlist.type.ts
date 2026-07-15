import { ContentResponse } from "./content.type.ts";
import { CursorResponse } from "./global.type.ts";

export interface UserSummary {
  id: string;
  name: string | null;
  profileImageUrl: string | null;
}

export interface PlaylistResponse {
  id: string;
  owner: UserSummary;
  updatedAt: string;
  title: string;
  description: string;
  subscriberCount: number;
  subscribedByMe: boolean;
  contents: ContentResponse[];
}

export type PlaylistListResponse = CursorResponse<PlaylistResponse>;

export interface PlaylistCursorRequestParams {
  limit: number;
  sortDirection: string;
  sortBy: string;
  cursor?: string | null;
  idAfter?: string | null;
  keywordLike?: string;
  ownerIdEqual?: string;
  subscriberIdEqual?: string;
}

// k6 에서는 전역 URLSearchParams가 기본적으로 없거나 버전/환경에 따라 ReferenceError가 날 수 있으므로 수동으로 조립합니다.
export function buildPlaylistQuery(params: PlaylistCursorRequestParams): string {
  const pairs: string[] = [];
  pairs.push(`limit=${encodeURIComponent(String(params.limit ?? 10))}`);
  pairs.push(`sortBy=${encodeURIComponent(params.sortBy ?? "updatedAt")}`);
  pairs.push(`sortDirection=${encodeURIComponent(params.sortDirection ?? "DESC")}`);
  if (params.cursor) pairs.push(`cursor=${encodeURIComponent(params.cursor)}`);
  if (params.idAfter) pairs.push(`idAfter=${encodeURIComponent(params.idAfter)}`);
  if (params.keywordLike) pairs.push(`keywordLike=${encodeURIComponent(params.keywordLike)}`);
  if (params.ownerIdEqual) pairs.push(`ownerIdEqual=${encodeURIComponent(params.ownerIdEqual)}`);
  if (params.subscriberIdEqual) pairs.push(`subscriberIdEqual=${encodeURIComponent(params.subscriberIdEqual)}`);
  return pairs.join("&");
}

