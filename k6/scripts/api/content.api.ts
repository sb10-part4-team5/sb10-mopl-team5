import config from '../config.ts';
import { get } from '../utils/http-client.ts';
import { CursorResponse, ContentResponse } from '../types/content.type.ts';

// ContentCursorRequest.java 대응 (limit/sortDirection/sortBy 는 필수)
export interface ContentListParams {
  limit: number;
  sortBy: string;
  sortDirection: string;
  typeEqual?: string;
  keywordLike?: string;
  tagsIn?: string[];
  cursor?: string;
  idAfter?: string;
}

// k6 런타임엔 URLSearchParams 가 없어 직접 인코딩한다 (배열은 key=v1&key=v2 반복 — Spring List<String> 바인딩과 호환)
function buildQuery(params: ContentListParams): string {
  const parts: string[] = [
    `limit=${params.limit}`,
    `sortBy=${params.sortBy}`,
    `sortDirection=${params.sortDirection}`,
  ];
  if (params.typeEqual) parts.push(`typeEqual=${encodeURIComponent(params.typeEqual)}`);
  if (params.keywordLike) parts.push(`keywordLike=${encodeURIComponent(params.keywordLike)}`);
  if (params.tagsIn?.length) {
    for (const t of params.tagsIn) parts.push(`tagsIn=${encodeURIComponent(t)}`);
  }
  if (params.cursor) parts.push(`cursor=${encodeURIComponent(params.cursor)}`);
  if (params.idAfter) parts.push(`idAfter=${encodeURIComponent(params.idAfter)}`);
  return parts.join('&');
}

// GET /api/contents (커서 목록 조회)
// tag: 지표 태그. 기본은 config.tags.content.list 지만, 조건별 비교 시나리오에서는
//      케이스별로 다른 라벨(예: 'GET /api/contents (keyword)')을 넘겨 리포트에서 행을 분리한다.
export function getContents(
  token: string,
  params: ContentListParams,
  tag: string = config.tags.content.list,
): CursorResponse<ContentResponse> | null {
  const url = `${config.endpoints.content.list}?${buildQuery(params)}`;
  return get<CursorResponse<ContentResponse>>(url, { token, tag });
}

// GET /api/contents/{contentId} (단건 조회)
export function getContent(token: string, contentId: string): ContentResponse | null {
  const url = config.endpoints.content.detail.replace('{contentId}', contentId);
  return get<ContentResponse>(url, { token, tag: config.tags.content.detail });
}
