import http from 'k6/http';
import config from '../config.ts';
import { LoginResponse } from '../types/auth.type.ts';

// CSRF: 쿠키(XSRF-TOKEN)로 받아 X-XSRF-TOKEN 헤더로 raw 전송 (XOR 아님)
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';

// 로그인 전 CSRF 쿠키 발급
// csrfToken이 필요한 요청이면 CSRF 쿠키 발급
export function fetchCsrfToken(): string {
  const res = http.get(config.endpoints.auth.csrfToken, {
    tags: { name: config.tags.auth.csrfToken },
  });

  const cookies = http.cookieJar().cookiesForURL(config.baseUrl);
  const token = cookies[CSRF_COOKIE_NAME]?.[0];

  if (!token) {
    throw new Error(
      `CSRF 토큰을 쿠키에서 찾지 못했습니다. status=${res.status} body=${res.body}`,
    );
  }

  // 모든 경로에서 쿠키가 전송되도록 base URL 기준으로 명시 재설정
  http.cookieJar().set(config.baseUrl, CSRF_COOKIE_NAME, token);

  return token;
}

// CSRF 발급 + 로그인 수행, 응답 전체(JwtResponse) 반환
function authenticate(email: string, password: string): LoginResponse {
  const csrfToken = fetchCsrfToken();

  const res = http.post(
    config.endpoints.auth.signIn,
    { username: email, password }, // 객체 → k6가 form-urlencoded 로 인코딩
    {
      headers: { [CSRF_HEADER_NAME]: csrfToken },
      tags: { name: config.tags.auth.signIn },
    },
  );

  if (res.status !== 200) {
    throw new Error(`로그인 실패: status=${res.status} body=${res.body}`);
  }

  return res.json() as unknown as LoginResponse;
}

// 로그인 -> accessToken 반환
export function login(email: string, password: string): string {
  return authenticate(email, password).accessToken;
}

// 순번 계정(user{index}@loadtest.local)으로 로그인. index는 1-based.
export function loginByIndex(index: number): string {
  return login(config.loadTestAccount.email(index), config.loadTestAccount.password);
}

// 로그인한 계정 자신의 userId까지 필요한 시나리오용 (Follow/DM처럼 "다른 시딩 계정의 UUID"가
// 필요한 경우, 관리자 전용 유저 목록 조회 없이 로그인 응답의 userDto.id로 대체한다)
export function loginByIndexWithProfile(index: number): { token: string; userId: string } {
  const body = authenticate(config.loadTestAccount.email(index), config.loadTestAccount.password);
  return { token: body.accessToken, userId: body.userDto.id };
}
