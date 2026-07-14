import http, { RefinedResponse, ResponseType, StructuredRequestBody } from 'k6/http';
import config from '../config.ts';

// 공통 HTTP 래퍼 (요청 + 상태 검증 + 에러 로깅 + 타입 파싱)

// 쓰기 요청(POST/PATCH/DELETE) 전용 CSRF 처리. setup()과 VU는 쿠키 저장소가 분리돼 있어
// 로그인 때 받은 쿠키가 VU에서 안 보이므로, 없으면 여기서 한 번 재발급한다.
// 시나리오 options에 noCookiesReset: true 필요 (안 그러면 반복마다 재발급됨).
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';

function csrfHeader(): Record<string, string> {
  let token = http.cookieJar().cookiesForURL(config.baseUrl)[CSRF_COOKIE_NAME]?.[0];
  if (!token) {
    http.get(config.endpoints.auth.csrfToken, { tags: { name: config.tags.auth.csrfToken } });
    token = http.cookieJar().cookiesForURL(config.baseUrl)[CSRF_COOKIE_NAME]?.[0];
  }
  return token ? { [CSRF_HEADER_NAME]: token } : {};
}

interface RequestOptions {
  token?: string | null; // 인증 필요한 요청용 (조회는 생략)
  tag?: string;          // 지표 태그 (config.tags.*)
}

// withCsrf=true인 경우에만 CSRF 헤더 부착 (GET은 불필요)
function buildParams(options?: RequestOptions, withCsrf = false) {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(withCsrf ? csrfHeader() : {}),
  };
  if (options?.token) {
    headers['Authorization'] = `Bearer ${options.token}`;
  }
  const params: { headers: Record<string, string>; tags?: { name: string } } = { headers };
  if (options?.tag) {
    params.tags = { name: options.tag };
  }
  return params;
}

// 2xx 여부 반환. non-2xx면 로깅만 하고 false → 호출부는 파싱 없이 null 반환
// multipart 등 buildParams를 못 쓰는 커스텀 요청(예: patchMultipart)에서도 재사용하도록 export
export function isOk(res: RefinedResponse<ResponseType | undefined>, method: string): boolean {
  if (res.status >= 200 && res.status < 300) {
    return true;
  }
  console.error(
    `[${method} 에러] status=${res.status} url=${res.request.url} body=${res.body}`,
  );
  return false;
}

// 본문 JSON 파싱. 없거나 비JSON이면 null + 경고 (VU 중단 방지)
export function parseJson<T>(res: RefinedResponse<ResponseType | undefined>, method: string): T | null {
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
  const res = http.post(url, payload, buildParams(options, true));
  if (!isOk(res, 'POST')) {
    return null;
  }
  return parseJson<T>(res, 'POST');
}

export function patch<T>(url: string, body: unknown, options?: RequestOptions): T | null {
  const payload = body ? JSON.stringify(body) : '';
  const res = http.patch(url, payload, buildParams(options, true));
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

// multipart/form-data 전용 PATCH (Content-Type은 k6가 자동 설정하므로 직접 지정 안 함)
export function patchMultipart<T>(
  url: string,
  fields: StructuredRequestBody,
  options?: RequestOptions,
): T | null {
  const headers: Record<string, string> = { ...csrfHeader() };
  if (options?.token) {
    headers['Authorization'] = `Bearer ${options.token}`;
  }
  const params: { headers: Record<string, string>; tags?: { name: string } } = { headers };
  if (options?.tag) {
    params.tags = { name: options.tag };
  }

  const res = http.patch(url, fields, params);
  if (!isOk(res, 'PATCH')) {
    return null;
  }
  return parseJson<T>(res, 'PATCH');
}
