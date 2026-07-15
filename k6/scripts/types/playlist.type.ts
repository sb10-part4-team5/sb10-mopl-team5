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
  limit?: number;
  sortDirection?: string;
  sortBy?: string;
  cursor?: string | null;
  idAfter?: string | null;
  keywordLike?: string;
  ownerIdEqual?: string;
  subscriberIdEqual?: string;
}

export function buildPlaylistQuery(params: PlaylistCursorRequestParams): string {
  const parts: string[] = [
    `limit=${params.limit ?? 10}`,
    `sortBy=${params.sortBy ?? "updatedAt"}`,
    `sortDirection=${params.sortDirection ?? "DESC"}`,
  ];
  if (params.cursor) parts.push(`cursor=${encodeURIComponent(params.cursor)}`);
  if (params.idAfter) parts.push(`idAfter=${encodeURIComponent(params.idAfter)}`);
  if (params.keywordLike) parts.push(`keywordLike=${encodeURIComponent(params.keywordLike)}`);
  if (params.ownerIdEqual) parts.push(`ownerIdEqual=${encodeURIComponent(params.ownerIdEqual)}`);
  if (params.subscriberIdEqual) parts.push(`subscriberIdEqual=${encodeURIComponent(params.subscriberIdEqual)}`);
  return parts.join("&");
}
