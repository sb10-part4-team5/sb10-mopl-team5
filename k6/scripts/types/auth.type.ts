// UserResponse.java 대응 (JwtResponse.userDto 로 중첩됨)
export interface LoginUserSummary {
  id: string;
  createdAt: string;
  email: string;
  name: string;
  profileImageUrl: string | null;
  role: string;
  locked: boolean;
}

// JwtResponse.java 에 대응
export interface LoginResponse {
  userDto: LoginUserSummary;
  accessToken: string;
}
