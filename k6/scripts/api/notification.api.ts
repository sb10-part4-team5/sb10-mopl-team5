import config from '../config.ts';
import { get, del } from '../utils/http-client.ts';
import { NotificationListResponse, NotificationResponse } from '../types/notification.type.ts';

// GET /api/notifications?limit=&cursor=&idAfter=&sortDirection=&sortBy=
export function getNotifications(
  token: string,
  params: {
    limit?: number;
    cursor?: string;
    idAfter?: string;
    sortDirection?: 'ASCENDING' | 'DESCENDING';
    sortBy?: string;
  } = {},
): NotificationListResponse | null {
  const qs = Object.entries({
    limit: params.limit ?? 20,
    ...(params.cursor ? { cursor: params.cursor } : {}),
    ...(params.idAfter ? { idAfter: params.idAfter } : {}),
    sortDirection: params.sortDirection ?? 'DESCENDING',
    sortBy: params.sortBy ?? 'createdAt',
  })
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join('&');

  const url = `${config.endpoints.notification.list}?${qs}`;
  return get<NotificationListResponse>(url, { token, tag: config.tags.notification.list });
}

// DELETE /api/notifications/{notificationId} — 읽음 처리
export function markNotificationRead(token: string, notificationId: string): boolean {
  const url = config.endpoints.notification.detail.replace('{notificationId}', notificationId);
  const res = del(url, { token, tag: config.tags.notification.markRead });
  return res.status >= 200 && res.status < 300;
}
