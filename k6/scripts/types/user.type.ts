// UserUpdateRequest.java 대응
export interface UserUpdateRequest {
  name: string;
}

// UserResponse.java
export interface UserResponse {
  id: string;
  createdAt: string;
  email: string;
  name: string;
  profileImageUrl: string | null;
  role: string;
  locked: boolean;
}
