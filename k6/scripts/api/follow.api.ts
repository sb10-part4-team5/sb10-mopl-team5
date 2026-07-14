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

// DELETE /api/follows/{followId} — 실패를 호출부가 알 수 있도록 성공 여부를 반환한다.
// 정리 실패를 조용히 넘기면 다음 반복의 createFollow가 409로 막혀 원인 파악이 어려워진다.
// (del()이 이미 실패 로깅을 하므로 여기서는 상태 코드만 확인하고 중복 로깅하지 않는다)
export function deleteFollow(token: string, followId: string): boolean {
  const url = config.endpoints.follow.detail.replace('{followId}', followId);
  const res = del(url, { token, tag: config.tags.follow.delete });
  return res.status >= 200 && res.status < 300;
}
