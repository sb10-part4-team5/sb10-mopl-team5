// FollowCreateRequest.java 대응
export interface FollowCreateRequest {
  followeeId: string;
}

// FollowResponse.java
export interface FollowResponse {
  id: string;
  followeeId: string;
  followerId: string;
}
