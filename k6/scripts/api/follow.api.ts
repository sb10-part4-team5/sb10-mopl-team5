import config from '../config.ts';
import { get, post, del } from '../utils/http-client.ts';
import { FollowResponse } from '../types/follow.type.ts';

// POST /api/follows
export function createFollow(token: string, followeeId: string): FollowResponse | null {
  return post<FollowResponse>(
    config.endpoints.follow.create,
    { followeeId },
    { token, tag: config.tags.follow.create },
  );
}

// GET /api/follows/followed-by-me?followeeId=
export function getFollowedByMe(token: string, followeeId: string): FollowResponse | null {
  const url = `${config.endpoints.follow.followedByMe}?followeeId=${followeeId}`;
  return get<FollowResponse>(url, { token, tag: config.tags.follow.followedByMe });
}

// GET /api/follows/count?followeeId= (인증 필요 — SecurityConfig가 /api/follows/** 통째로 authenticated)
export function getFollowerCount(token: string, followeeId: string): number | null {
  const url = `${config.endpoints.follow.count}?followeeId=${followeeId}`;
  return get<number>(url, { token, tag: config.tags.follow.count });
}

// DELETE /api/follows/{followId}
export function deleteFollow(token: string, followId: string): void {
  const url = config.endpoints.follow.detail.replace('{followId}', followId);
  del(url, { token, tag: config.tags.follow.delete });
}
