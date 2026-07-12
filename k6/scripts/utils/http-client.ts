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

// 2xx 여부 반환. non-2xx면 상태·본문을 로깅한다.
// 호출부는 false일 때 본문을 파싱하지 말고 null 을 반환해야 한다
// (에러 응답이 비JSON(예: 401 HTML)일 때 res.json() 이 던지는 "JSON parse error"로
//  실제 원인(HTTP 상태)이 가려지는 것을 방지).
function isOk(res: RefinedResponse<ResponseType | undefined>, method: string): boolean {
  if (res.status >= 200 && res.status < 300) {
    return true;
  }
  console.error(
    `[${method} 에러] status=${res.status} url=${res.request.url} body=${res.body}`,
  );
  return false;
}

// 2xx 응답 본문을 안전하게 JSON 파싱. 본문이 없거나 비JSON이면 null + 경고 (VU 중단 방지).
function parseJson<T>(res: RefinedResponse<ResponseType | undefined>, method: string): T | null {
  if (!res.body) {
    return null;
  }
  try {
    return res.json() as unknown as T;
  } catch (e) {
    console.error(
      `[${method} 파싱 실패] 2xx 이지만 본문이 JSON이 아닙니다. status=${res.status} body=${res.body}`,
    );
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
