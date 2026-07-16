import http from 'k6/http';
import config from '../config.ts';
import { SseConnectionResult } from "../types/sse.type.ts";

// GET /api/sse — SSE 연결 수립 후 holdDuration 동안 유지하다가 종료.
// SSE 스트림은 서버가 닫기 전까지 끝나지 않으므로 k6 타임아웃으로 연결 시간을 제어한다.
// error_code 1050(request timeout)은 의도적 종료이므로 성공으로 분류한다.
export function connectSse(token: string, holdDuration: string = '5s'): SseConnectionResult {
  const res = http.get(config.endpoints.sse.subscribe, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream',
      'Cache-Control': 'no-cache',
    },
    timeout: holdDuration,
    tags: { name: config.tags.sse.subscribe },
  });

  const timedOut = res.error_code === 1050; // 에러코드 1050 -> 타임아웃으로 연결 종료
  const connected = res.status === 200 || timedOut; // 200 응답을 받거나 timedOut이 일어나면 연결 성공이라는 의미

  if (!connected) {
    console.error(
      `[SSE 연결 실패] status=${res.status} error_code=${res.error_code} error=${res.error}`,
    );
  }

  return {
    connected,
    timedOut,
    waitingMs: res.timings.waiting,
  };
}
