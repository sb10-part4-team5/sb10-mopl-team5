export interface SseConnectionResult {
  connected: boolean;
  timedOut: boolean;
  waitingMs: number;
}
