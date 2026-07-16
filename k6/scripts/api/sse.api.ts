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
  // k6가 SSE를 타임아웃으로 abort하면 res.status는 200이 아닌 0으로 집계되므로
  // timedOut(정상 유지 후 종료)도 연결 성공으로 분류해야 한다
  const connected = timedOut || res.status === 200;

  const body = typeof res.body === 'string' ? res.body : '';
  // Spring SseEmitter는 "event:name" 형식(콜론 뒤 공백 없음)으로 전송한다
  // connect 이벤트: "data:connected" 라인으로 판별 (event 필드 공백 여부와 무관)
  const hasConnectEvent = body.includes('data:connected') || body.includes('data: connected');
  // notifications 이벤트 수: "event:notifications" 출현 횟수 카운트
  const missedNotificationCount = body.split('event:notifications').length - 1;

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
    missedNotificationCount,
  };
}
