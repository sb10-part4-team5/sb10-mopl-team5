import { check } from 'k6';
import http from 'k6/http';
import exec from 'k6/execution';
import config from '../config.ts';
import { LoginResponse } from '../types/auth.type.ts';
import { summaryHandler } from '../utils/reporter.ts';

const VUS = Number(__ENV.VUS || 10);

const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';

type SetupData = {
  csrfTokens: string[];
};

export const options = {
  scenarios: {
    auth_sign_in_only: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  thresholds: {
    [`http_req_failed{name:${config.tags.auth.signIn}}`]: [
      'rate<0.01',
    ],
    [`http_req_duration{name:${config.tags.auth.signIn}}`]: [
      'p(95)<500',
    ],
    [`http_reqs{name:${config.tags.auth.signIn}}`]: [
      'count>0',
    ],
  },
};

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

function signInOnly(
    email: string,
    password: string,
    csrfToken: string,
): string {
  const jar = http.cookieJar();

  // CSRF 토큰은 헤더뿐 아니라 쿠키에도 존재해야 함
  jar.set(config.baseUrl, CSRF_COOKIE_NAME, csrfToken);

  const res = http.post(
      config.endpoints.auth.signIn,
      {
        username: email,
        password,
      },
      {
        headers: {
          [CSRF_HEADER_NAME]: csrfToken,
        },
        tags: {
          name: config.tags.auth.signIn,
        },
      },
  );

  if (res.status !== 200) {
    throw new Error(
        `로그인 실패: email=${email} status=${res.status} body=${res.body}`,
    );
  }

  const body = res.json() as unknown as LoginResponse;

  if (!body.accessToken) {
    throw new Error(
        `로그인 응답에 accessToken이 없습니다. email=${email}`,
    );
  }

  return body.accessToken;
}

export function setup(): SetupData {
  const csrfTokens: string[] = [];

  for (let i = 0; i < VUS; i++) {
    csrfTokens.push(fetchCsrfToken());
  }

  if (csrfTokens.length !== VUS) {
    throw new Error(
        `[setup] CSRF 토큰 준비 실패: expected=${VUS}, actual=${csrfTokens.length}`,
    );
  }

  console.log(`[setup] CSRF 토큰 ${csrfTokens.length}개 준비 완료`);

  return { csrfTokens };
}

export default function (data: SetupData): void {
  if (data.csrfTokens.length === 0) {
    throw new Error('[VU] 사용할 CSRF 토큰이 없습니다.');
  }

  const accountIndex = exec.vu.idInTest;
  const csrfToken =
      data.csrfTokens[(accountIndex - 1) % data.csrfTokens.length];

  const accessToken = signInOnly(
      config.loadTestAccount.email(accountIndex),
      config.loadTestAccount.password,
      csrfToken,
  );

  check(accessToken, {
    'accessToken 발급 성공': (token) =>
        typeof token === 'string' && token.length > 0,
  });
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'summary-auth-sign-in-only.html');
}
