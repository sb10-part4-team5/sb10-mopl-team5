import { check } from "k6";
import exec from "k6/execution";
import { getPlaylist, getPlaylists } from "../api/playlist.api.ts";
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
    ...endpointThresholds(config.tags.playlist.list, ["p(95)<500"], "load"),
    ...endpointThresholds(config.tags.playlist.detail, ["p(95)<500"], "load"),
    ...endpointThresholds(config.tags.auth.csrfToken, ["p(95)<300"]),
    ...endpointThresholds(config.tags.auth.signIn, ["p(95)<800"]),
  },
};

type SetupData = { token: string; userId: string }[];

export function setup(): SetupData {
  return setupAuthWithProfile(TARGET_VUS);
}

export function run(data: SetupData): void {
  if (data.length === 0) {
    throw new Error("[VU] 로그인된 계정이 없습니다. setup()을 확인하세요.");
  }
  const account = data[(exec.vu.idInTest - 1) % data.length];

  // 1. 재생목록 커서 페이징 조회 (최대 3페이지)
  let cursor: string | null | undefined = undefined;
  let idAfter: string | null | undefined = undefined;
  let firstPlaylistId: string | null = null;

  for (let i = 0; i < 3; i++) {
    const listRes = getPlaylists(account.token, { cursor, idAfter });
    
    check(listRes, {
      "재생목록 페이징 조회 성공": (r) => r !== null,
    });

    if (!listRes) break;

    // 첫 번째 페이지에서 첫 재생목록 ID 추출 (상세 조회를 위함)
    if (i === 0 && listRes.data && listRes.data.length > 0) {
      firstPlaylistId = listRes.data[0].id;
    }

    if (!listRes.hasNext) break;
    cursor = listRes.nextCursor;
    idAfter = listRes.nextIdAfter;
  }

  randomThinkTime(1, 2);

  // 2. 재생목록 단건 상세 조회
  if (firstPlaylistId) {
    const detailRes = getPlaylist(account.token, firstPlaylistId);
    check(detailRes, {
      "재생목록 상세 조회 성공": (r) => r !== null && r.id === firstPlaylistId,
    });
  }

  randomThinkTime(1, 3);
}

export function handleSummary(data: any) {
  return summaryHandler(data, "playlist-read-load-test-summary.html");
}
