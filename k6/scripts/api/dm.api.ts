import config from '../config.ts';
import { get, post } from '../utils/http-client.ts';
import { ConversationResponse, DirectMessageResponse } from '../types/dm.type.ts';
// CursorResponse는 도메인 공통 페이징 응답이라 content.type.ts에 있는 걸 그대로 재사용
import { CursorResponse } from '../types/content.type.ts';

export interface ConversationListParams {
  limit: number;
  sortDirection: string;
  keywordLike?: string;
  cursor?: string;
  idAfter?: string;
}

function buildQuery(params: ConversationListParams): string {
  const parts: string[] = [`limit=${params.limit}`, `sortDirection=${params.sortDirection}`];
  if (params.keywordLike) parts.push(`keywordLike=${encodeURIComponent(params.keywordLike)}`);
  if (params.cursor) parts.push(`cursor=${encodeURIComponent(params.cursor)}`);
  if (params.idAfter) parts.push(`idAfter=${encodeURIComponent(params.idAfter)}`);
  return parts.join('&');
}

// POST /api/conversations — 기존 대화방이 있으면 그대로 반환, 없으면 새로 생성
export function createOrGetConversation(token: string, withUserId: string): ConversationResponse | null {
  return post<ConversationResponse>(
    config.endpoints.conversation.list,
    { withUserId },
    { token, tag: config.tags.conversation.create },
  );
}

// GET /api/conversations (커서 목록)
export function getConversations(
  token: string,
  params: ConversationListParams,
): CursorResponse<ConversationResponse> | null {
  const url = `${config.endpoints.conversation.list}?${buildQuery(params)}`;
  return get<CursorResponse<ConversationResponse>>(url, { token, tag: config.tags.conversation.list });
}

// GET /api/conversations/{conversationId}/direct-messages (커서 목록)
// 주의: STOMP 전송 시나리오 없이는 메시지가 안 쌓여 있을 수 있어 빈 목록이 정상일 수 있음
export function getMessages(
  token: string,
  conversationId: string,
  limit: number,
  sortDirection: string,
): CursorResponse<DirectMessageResponse> | null {
  const url =
    `${config.endpoints.directMessage.list.replace('{conversationId}', conversationId)}` +
    `?limit=${limit}&sortDirection=${sortDirection}`;
  return get<CursorResponse<DirectMessageResponse>>(url, { token, tag: config.tags.directMessage.list });
}

// POST /api/conversations/{conversationId}/direct-messages/{directMessageId}/read
export function markMessageRead(token: string, conversationId: string, directMessageId: string): void {
  const url = config.endpoints.directMessage.read
    .replace('{conversationId}', conversationId)
    .replace('{directMessageId}', directMessageId);
  post<void>(url, null, { token, tag: config.tags.directMessage.read });
}
