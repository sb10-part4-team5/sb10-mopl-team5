import http, { RefinedResponse, ResponseType } from 'k6/http';

// 공통 HTTP 래퍼 (요청 + 상태 검증 + 에러 로깅 + 타입 파싱)

interface RequestOptions {
  token?: string | null;      // 인증 필요한 요청용 (조회는 생략)
  tag?: string;               // 지표 태그 (config.tags.*)
  csrfToken?: string | null;  // POST/PATCH/DELETE 에 필요한 CSRF 토큰
}

function buildParams(options?: RequestOptions) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (options?.token) {
    headers['Authorization'] = `Bearer ${options.token}`;
  }
  if (options?.csrfToken) {
    headers['X-XSRF-TOKEN'] = options.csrfToken;
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
    return null; // non-2xx: 에러 본문을 파싱하지 않음
  }
  return parseJson<T>(res, 'GET');
}

export function post<T>(url: string, body: unknown, options?: RequestOptions): T | null {
  const payload = body ? JSON.stringify(body) : '';
  const res = http.post(url, payload, buildParams(options));
  if (!isOk(res, 'POST')) {
    return null;
  }
  return parseJson<T>(res, 'POST');
}

export function patch<T>(url: string, body: unknown, options?: RequestOptions): T | null {
  const payload = body ? JSON.stringify(body) : '';
  const res = http.patch(url, payload, buildParams(options));
  if (!isOk(res, 'PATCH')) {
    return null;
  }
  return parseJson<T>(res, 'PATCH');
}

export function del(url: string, options?: RequestOptions): void {
  const res = http.del(url, null, buildParams(options));
  isOk(res, 'DELETE'); // 본문 파싱 없음. non-2xx면 로깅만.
}
