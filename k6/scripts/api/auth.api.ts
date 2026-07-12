import http from 'k6/http';
import config from '../config.ts';
import { LoginResponse } from '../types/auth.type.ts';

// SecurityConfig: CookieCsrfTokenRepository.withHttpOnlyFalse()
// SpaCsrfTokenRequestHandler: raw 값을 X-XSRF-TOKEN 헤더로 그대로 받음 (XOR 인코딩 아님)
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';

// 로그인(POST /api/auth/sign-in) 전에 CSRF 쿠키를 먼저 발급받아야 한다.
function fetchCsrfToken(): string {
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
  return token;
}

// 로그인 -> accessToken 반환 (formLogin: form-urlencoded, 파라미터명 username/password)
export function login(email: string, password: string): string {
  const csrfToken = fetchCsrfToken();

  const res = http.post(
    config.endpoints.auth.signIn,
    { username: email, password }, // 객체를 넘기면 k6가 자동으로 application/x-www-form-urlencoded 인코딩
    {
      headers: { [CSRF_HEADER_NAME]: csrfToken },
      tags: { name: config.tags.auth.signIn },
    },
  );

  if (res.status !== 200) {
    throw new Error(`로그인 실패: status=${res.status} body=${res.body}`);
  }

  const body = res.json() as unknown as LoginResponse;
  return body.accessToken;
}

// data-generator 로 시딩된 순번 계정(user{index}@loadtest.local)으로 로그인. index는 1-based.
export function loginByIndex(index: number): string {
  return login(config.loadTestAccount.email(index), config.loadTestAccount.password);
}
