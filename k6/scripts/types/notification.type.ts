import { CursorResponse } from './global.type.ts';

// NotificationLevel.java 대응
export type NotificationLevel = 'INFO' | 'WARNING' | 'ERROR';

// NotificationType.java 대응
export type NotificationType =
  | 'ROLE_CHANGED'
  | 'PLAYLIST_SUBSCRIBED'
  | 'PLAYLIST_UPDATED'
  | 'FOLLOWED'
  | 'DIRECT_MESSAGE'
  | 'WATCHING_ACTIVITY';

// NotificationResponse.java 대응
export interface NotificationResponse {
  id: string;
  createdAt: string;
  receiverId: string;
  title: string;
  content: string;
  level: NotificationLevel;
}

// GET /api/notifications 응답 (커서 페이징)
export type NotificationListResponse = CursorResponse<NotificationResponse>;
