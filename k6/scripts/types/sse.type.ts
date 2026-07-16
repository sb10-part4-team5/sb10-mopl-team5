export interface SseConnectionResult {
  connected: boolean;
  timedOut: boolean;
  waitingMs: number;
  hasConnectEvent: boolean;
  missedNotificationCount: number;
}
