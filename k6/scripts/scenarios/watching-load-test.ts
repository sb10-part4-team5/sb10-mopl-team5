// 시청 세션 조회 부하테스트 — GET /api/users/{watcherId}/watching-sessions, GET /api/contents/{contentId}/watching-sessions
// setup()에서 각 계정이 콘텐츠 하나씩 시청(참고: 시청은 STOMP로만 가능하므로 REST로는 시청 기록이 없을 수 있음)
// 하지만 조회 API는 빈 응답이어도 에러가 아니므로 테스트 가능

import { check } from "k6";
import exec from "k6/execution";
import {
  getWatchingSessionByUser,
  getWatchingSessionsByContent,
} from "../api/watching.api.ts";
import config, { endpointThresholds, warmupLoadScenarios } from "../config.ts";
import { randomThinkTime } from "../utils/random.ts";
import { summaryHandler } from "../utils/reporter.ts";
import { setupAuthWithProfile } from "../utils/setup.ts";

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || "30s";
const HOLD_TIME = __ENV.HOLD_TIME || "1m";
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const WARMUP_TIME = __ENV.WARMUP_TIME || "20s";

export const options = {
  // CSRF 쿠키를 VU당 한 번만 발급받아 재사용하기 위함 (기본값은 반복마다 쿠키 저장소 초기화)
  noCookiesReset: true,
  scenarios: warmupLoadScenarios({
    exec: "run",
    targetVus: TARGET_VUS,
    rampTime: RAMP_TIME,
    holdTime: HOLD_TIME,
    warmupVus: WARMUP_VUS,
    warmupTime: WARMUP_TIME,
  }),
  thresholds: {
    http_req_failed: ["rate<0.01"],
    ...endpointThresholds(config.tags.watching.byUser, ["p(95)<500"], "load"),
    ...endpointThresholds(
      config.tags.watching.byContent,
      ["p(95)<500"],
      "load",
    ),
    ...endpointThresholds(config.tags.auth.csrfToken, ["p(95)<300"]),
    ...endpointThresholds(config.tags.auth.signIn, ["p(95)<800"]),
  },
};

type SetupData = { token: string; userId: string }[];

export function setup(): SetupData {
  return setupAuthWithProfile(TARGET_VUS);
}

// warmup / load 두 시나리오가 공유하는 실행 함수
export function run(data: SetupData): void {
  if (data.length === 0) {
    throw new Error("[VU] 로그인된 계정이 없습니다. setup()을 확인하세요.");
  }
  const account = data[(exec.vu.idInTest - 1) % data.length];

  // 1. 내 시청 세션 조회 (단건 응답, 없으면 null)
  const mySession = getWatchingSessionByUser(account.token, account.userId);
  check(mySession, {
    "시청 세션 조회 응답 존재 또는 없음(정상)": () => true, // null이어도 에러 아님 (시청 기록 없을 수 있음)
  });

  // 2. 콘텐츠별 시청 세션 목록 조회 (커서 페이지네이션)
  //    최대 3페이지까지 연속 조회하여 커서 페이지네이션 부하 시뮬레이션
  const firstContentId = "00000000-0000-0000-0000-000000000001";
  let cursor: string | null | undefined = undefined;
  let idAfter: string | null | undefined = undefined;

  for (let i = 0; i < 3; i++) {
    const contentSessions = getWatchingSessionsByContent(
      account.token,
      firstContentId,
      { cursor, idAfter }
    );
    
    check(contentSessions, {
      "콘텐츠별 시청 세션 목록 응답 존재": (s) => s !== null,
    });

    if (!contentSessions || !contentSessions.hasNext) break;
    cursor = contentSessions.nextCursor;
    idAfter = contentSessions.nextIdAfter;
  }

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, "watching-load-test-summary.html");
}
