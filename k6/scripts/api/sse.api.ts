import http from 'k6/http';
import config from '../config.ts';
import { SseConnectionResult } from "../types/sse.type.ts";

// GET /api/sse — SSE 연결 수립 후 holdDuration 동안 유지하다가 종료.
// SSE 스트림은 서버가 닫기 전까지 끝나지 않으므로 k6 타임아웃으로 연결 시간을 제어한다.
// error_code 1050(request timeout)은 의도적 종료이므로 성공으로 분류한다.
export function connectSse(
  token: string,
  holdDuration: string = '5s',
  lastEventId?: string,
): SseConnectionResult {
  const headers: Record<string, string> = {
    Authorization: `Bearer ${token}`,
    Accept: 'text/event-stream',
    'Cache-Control': 'no-cache',
  };
  if (lastEventId) {
    headers['Last-Event-ID'] = lastEventId;
  }

  const res = http.get(config.endpoints.sse.subscribe, {
    headers,
    timeout: holdDuration,
    tags: { name: config.tags.sse.subscribe },
  });

  const timedOut = res.error_code === 1050; // 에러코드 1050 -> 타임아웃으로 연결 종료
  const connected = res.status === 200; // 정상 응답 헤더(200 OK)를 받아 연결이 수립되어야만 성공
  // 타임아웃 시 k6가 수신한 부분 body를 반환하므로 초기 connect 이벤트 포함 여부 확인 가능
  const hasConnectEvent = typeof res.body === 'string' && res.body.includes('connected');

  if (!connected) {
    console.error(
      `[SSE 연결 실패] status=${res.status} error_code=${res.error_code} error=${res.error}`,
    );
  }

  return {
    connected,
    timedOut,
    waitingMs: res.timings.waiting,
    hasConnectEvent,
  };
}
