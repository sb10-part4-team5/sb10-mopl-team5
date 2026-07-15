import config from "../config.ts";
import {
  WatchingSessionListResponse,
  WatchingSessionResponse,
  buildQuery,
} from "../types/watching.type.ts";
import { get } from "../utils/http-client.ts";

// GET /api/users/{watcherId}/watching-sessions (단건 조회 - 현재 로그인한 계정의 시청 세션)
export function getWatchingSessionByUser(
  token: string,
  watcherId: string,
): WatchingSessionResponse | null {
  const url = config.endpoints.watching.byUser.replace(
    "{watcherId}",
    watcherId,
  );
  return get<WatchingSessionResponse>(url, {
    token,
    tag: config.tags.watching.byUser,
  });
}

// GET /api/contents/{contentId}/watching-sessions (커서 목록 조회 - 특정 콘텐츠의 시청 세션 목록)
export function getWatchingSessionsByContent(
  token: string,
  contentId: string,
  params?: Partial<import("../types/watching.type.ts").WatchingSessionListParams>,
): WatchingSessionListResponse | null {
  const queryParams = buildQuery({
    limit: params?.limit ?? 10,
    sortDirection: params?.sortDirection ?? "DESC",
    sortBy: "createdAt", // watching 세션은 createdAt만 정렬 가능
    watcherNameLike: params?.watcherNameLike,
    cursor: params?.cursor,
    idAfter: params?.idAfter,
  });
  const url = `${config.endpoints.watching.byContent.replace("{contentId}", contentId)}?${queryParams}`;
  return get<WatchingSessionListResponse>(url, {
    token,
    tag: config.tags.watching.byContent,
  });
}
