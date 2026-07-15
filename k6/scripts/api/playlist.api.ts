import config from "../config.ts";
import { PlaylistListResponse, PlaylistResponse, PlaylistCursorRequestParams, buildPlaylistQuery } from "../types/playlist.type.ts";
import { get } from "../utils/http-client.ts";

// GET /api/playlists/{id} (단건 조회)
export function getPlaylist(
  token: string,
  id: string,
): PlaylistResponse | null {
  const url = config.endpoints.playlist.detail.replace("{id}", id);
  return get<PlaylistResponse>(url, {
    token,
    tag: config.tags.playlist.detail,
  });
}

// GET /api/playlists (커서 기반 목록 조회)
export function getPlaylists(
  token: string,
  params?: PlaylistCursorRequestParams,
): PlaylistListResponse | null {
  const queryParams = buildPlaylistQuery(params || {});
  const url = `${config.endpoints.playlist.list}?${queryParams}`;
  return get<PlaylistListResponse>(url, {
    token,
    tag: config.tags.playlist.list,
  });
}
