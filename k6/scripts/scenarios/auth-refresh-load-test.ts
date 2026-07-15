import { check, sleep } from 'k6';
import exec from 'k6/execution';
import http from 'k6/http';
import { Rate, Trend } from 'k6/metrics';
import config from '../config.ts';
import { loginByIndex } from '../api/auth.api.ts';
import { LoginResponse } from '../types/auth.type.ts';
import { summaryHandler } from '../utils/reporter.ts';

const VUS = Number(__ENV.VUS || 10);
const REFRESH_TOKEN_COOKIE = 'REFRESH_TOKEN';
const MAX_DEBUG_LOGS_PER_VU = 3;

const refreshDuration = new Trend('refresh_duration', true);
const refreshSuccessRate = new Rate('refresh_success_rate');

let loginCompleted = false;
let debugLogCount = 0;

export const options = {
  noCookiesReset: true,

  scenarios: {
    auth_refresh: {
      executor: 'constant-vus',
      vus: VUS,
      duration: '30s',
    },
  },

  thresholds: {
    refresh_duration: ['p(95)<500'],
    refresh_success_rate: ['rate>0.99'],
    [`http_req_duration{name:${config.tags.auth.refresh}}`]: ['p(95)<500'],
    [`http_req_failed{name:${config.tags.auth.refresh}}`]: ['rate<0.01'],
    [`http_reqs{name:${config.tags.auth.refresh}}`]: ['count>0'],
  },
};

function getRefreshTokensFromJar(): string[] {
  const cookies = http.cookieJar().cookiesForURL(
      config.endpoints.auth.refresh,
  );

  const refreshTokens = cookies[REFRESH_TOKEN_COOKIE];

  if (!Array.isArray(refreshTokens)) {
    return [];
  }

  return refreshTokens.filter(
      (token): token is string =>
          typeof token === 'string' && token.length > 0,
  );
}

function getRefreshTokenFromJar(): string | null {
  return getRefreshTokensFromJar()[0] ?? null;
}

function loginOncePerVu(): void {
  if (loginCompleted) {
    return;
  }

  const accountIndex = exec.vu.idInTest;

  loginByIndex(accountIndex);

  const refreshToken = getRefreshTokenFromJar();

  if (!refreshToken) {
    throw new Error(
        `로그인 응답의 cookie jar에 ${REFRESH_TOKEN_COOKIE}이 없습니다. `
        + `accountIndex=${accountIndex}`,
    );
  }

  loginCompleted = true;
}

function logDebug(
    res: ReturnType<typeof http.post>,
    previousTokenExists: boolean,
    currentTokenExists: boolean,
): void {
  if (debugLogCount >= MAX_DEBUG_LOGS_PER_VU) {
    return;
  }

  debugLogCount += 1;

  const responseBody = String(res.body ?? '');
  const limitedBody =
      responseBody.length > 500
          ? `${responseBody.substring(0, 500)}...`
          : responseBody;

  console.error(
      [
        '[refresh debug]',
        `vu=${exec.vu.idInTest}`,
        `status=${res.status}`,
        `previousTokenExists=${previousTokenExists}`,
        `currentTokenExists=${currentTokenExists}`,
        `setCookie=${String(res.headers['Set-Cookie'] ?? '')}`,
        `body=${limitedBody}`,
      ].join(' '),
  );
}

export default function (): void {
  loginOncePerVu();

  const previousRefreshToken = getRefreshTokenFromJar();

  const res = http.post(
      config.endpoints.auth.refresh,
      null,
      {
        tags: {
          name: config.tags.auth.refresh,
        },
      },
  );

  const currentRefreshToken = getRefreshTokenFromJar();

  let body: LoginResponse | null = null;

  if (res.status === 200) {
    try {
      body = res.json() as unknown as LoginResponse;
    } catch {
      body = null;
    }
  }

  const setCookie = String(res.headers['Set-Cookie'] ?? '');

  const statusOk = res.status === 200;

  const accessTokenExists =
      typeof body?.accessToken === 'string'
      && body.accessToken.length > 0;

  const userExists =
      body?.userDto !== null
      && body?.userDto !== undefined
      && typeof body.userDto === 'object';

  const refreshCookieReturned =
      setCookie.includes(`${REFRESH_TOKEN_COOKIE}=`);

  const refreshCookieInJar =
      currentRefreshToken !== null;

  const refreshTokenRotated =
      previousRefreshToken !== null
      && currentRefreshToken !== null
      && previousRefreshToken !== currentRefreshToken;

  const succeeded =
      statusOk
      && accessTokenExists
      && userExists
      && refreshCookieReturned
      && refreshCookieInJar
      && refreshTokenRotated;

  refreshDuration.add(res.timings.duration);
  refreshSuccessRate.add(succeeded);

  check(res, {
    'refresh status is 200': () => statusOk,
    'refresh accessToken exists': () => accessTokenExists,
    'refresh userDto exists': () => userExists,
    'refresh Set-Cookie contains REFRESH_TOKEN': () =>
        refreshCookieReturned,
    'refresh cookie jar contains REFRESH_TOKEN': () =>
        refreshCookieInJar,
    'refresh token is rotated': () =>
        refreshTokenRotated,
  });

  sleep(1);

  if (!succeeded) {
    logDebug(
        res,
        previousRefreshToken !== null,
        currentRefreshToken !== null,
    );
  }
}

export function handleSummary(data: any) {
  return summaryHandler(
      data,
      `refresh-vu-${VUS}.html`,
  );
}
