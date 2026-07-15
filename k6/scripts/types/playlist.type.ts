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

// k6 v0.40.0+부터 URLSearchParams API를 공식 지원하므로 표준 방식을 사용한다
export function buildPlaylistQuery(params: PlaylistCursorRequestParams): string {
  const searchParams = new URLSearchParams();
  searchParams.set("limit", String(params.limit ?? 10));
  searchParams.set("sortBy", params.sortBy ?? "updatedAt");
  searchParams.set("sortDirection", params.sortDirection ?? "DESC");
  if (params.cursor) searchParams.set("cursor", params.cursor);
  if (params.idAfter) searchParams.set("idAfter", params.idAfter);
  if (params.keywordLike) searchParams.set("keywordLike", params.keywordLike);
  if (params.ownerIdEqual) searchParams.set("ownerIdEqual", params.ownerIdEqual);
  if (params.subscriberIdEqual) searchParams.set("subscriberIdEqual", params.subscriberIdEqual);
  return searchParams.toString();
}

