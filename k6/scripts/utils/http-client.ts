import http, { RefinedResponse, ResponseType } from 'k6/http';

// 공통 HTTP 래퍼 (요청 + 상태 검증 + 에러 로깅 + 타입 파싱)

// csrf 토큰 관련 값들
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

interface RequestOptions {
  token?: string | null; // 인증 필요한 요청용 (조회는 생략)
  tag?: string;          // 지표 태그 (config.tags.*)
}

function buildParams(options?: RequestOptions, withCsrf = false) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (options?.token) {
    headers['Authorization'] = `Bearer ${options.token}`;
  }
  if (withCsrf) { // csrf 보호가 필요한 요청인 경우
    const cookies = http.cookieJar().cookiesForURL(BASE_URL); // 현재 CookieJar에 저장되어 있는 쿠키를 읽는다
    const csrfToken = cookies[CSRF_COOKIE_NAME]?.[0]; // CSRF 쿠키에서 토큰 값을 가져온다.
    if (csrfToken) {
      headers[CSRF_HEADER_NAME] = csrfToken; // CSRF 토큰을 요청 헤더에 추가한다.
    }
  }
  const params: { headers: Record<string, string>; tags?: { name: string } } = { headers };
  if (options?.tag) {
    params.tags = { name: options.tag };
  }
  return params;
}

// 2xx 여부 반환. non-2xx면 로깅만 하고 false → 호출부는 파싱 없이 null 반환
function isOk(res: RefinedResponse<ResponseType | undefined>, method: string): boolean {
  if (res.status >= 200 && res.status < 300) {
    return true;
  }
  console.error(
    `[${method} 에러] status=${res.status} url=${res.request.url} body=${res.body}`,
  );
  return false;
}

// 본문 JSON 파싱. 없거나 비JSON이면 null + 경고 (VU 중단 방지)
function parseJson<T>(res: RefinedResponse<ResponseType | undefined>, method: string): T | null {
  if (!res.body) {
    return null;
  }
  try {
    return res.json() as unknown as T;
  } catch (e) {
    console.error(`[${method} 파싱 실패] 2xx 이지만 비JSON. status=${res.status} body=${res.body}`);
    return null;
  }
}

export function get<T>(url: string, options?: RequestOptions): T | null {
  const res = http.get(url, buildParams(options));
  if (!isOk(res, 'GET')) {
    return null;
  }
  return parseJson<T>(res, 'GET');
}

export function post<T>(url: string, body: unknown, options?: RequestOptions): T | null {
  const payload = body ? JSON.stringify(body) : '';
  const res = http.post(url, payload, buildParams(options, true)); // POST 요청에는 CSRF 헤더를 포함한다.
  if (!isOk(res, 'POST')) {
    return null;
  }
  return parseJson<T>(res, 'POST');
}

export function patch<T>(url: string, body: unknown, options?: RequestOptions): T | null {
  const payload = body ? JSON.stringify(body) : '';
  const res = http.patch(url, payload, buildParams(options, true)); // PATCH 요청에는 CSRF 헤더를 포함한다.
  if (!isOk(res, 'PATCH')) {
    return null;
  }
  return parseJson<T>(res, 'PATCH');
}

export function del(url: string, options?: RequestOptions): RefinedResponse<ResponseType | undefined> {
  const res = http.del(url, null, buildParams(options, true)); // DELETE 요청에는 CSRF 헤더를 포함한다.
  isOk(res, 'DELETE');
  return res;
}
