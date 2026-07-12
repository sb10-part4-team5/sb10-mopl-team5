import http, { RefinedResponse, ResponseType } from 'k6/http';

// 공통 HTTP 래퍼 — 요청 전송 + 상태코드 검증 + 에러 로깅 + 타입 파싱
// 조회/쓰기 시나리오가 공통으로 사용한다.

interface RequestOptions {
  token?: string | null; // 인증이 필요한 요청(POST/PATCH/DELETE)용. 조회는 생략.
  tag?: string;          // 지표 태그 (config.tags.* 사용)
}

function buildParams(options?: RequestOptions) {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (options?.token) {
    headers['Authorization'] = `Bearer ${options.token}`;
  }
  const params: { headers: Record<string, string>; tags?: { name: string } } = { headers };
  if (options?.tag) {
    params.tags = { name: options.tag };
  }
  return params;
}

function ensureOk(res: RefinedResponse<ResponseType | undefined>, method: string): void {
  if (res.status >= 200 && res.status < 300) {
    return;
  }
  console.error(
    `[${method} 에러] status=${res.status} url=${res.request.url} body=${res.body}`,
  );
}

export function get<T>(url: string, options?: RequestOptions): T | null {
  const res = http.get(url, buildParams(options));
  ensureOk(res, 'GET');
  return res.body ? (res.json() as unknown as T) : null;
}

export function post<T>(url: string, body: unknown, options?: RequestOptions): T | null {
  const payload = body ? JSON.stringify(body) : '';
  const res = http.post(url, payload, buildParams(options));
  ensureOk(res, 'POST');
  return res.body ? (res.json() as unknown as T) : null;
}

export function patch<T>(url: string, body: unknown, options?: RequestOptions): T | null {
  const payload = body ? JSON.stringify(body) : '';
  const res = http.patch(url, payload, buildParams(options));
  ensureOk(res, 'PATCH');
  return res.body ? (res.json() as unknown as T) : null;
}

export function del(url: string, options?: RequestOptions): void {
  const res = http.del(url, null, buildParams(options));
  ensureOk(res, 'DELETE');
}
