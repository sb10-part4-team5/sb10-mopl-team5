// 프로필 수정 부하테스트 — PATCH /api/users/{userId} (multipart/form-data)
// 이미지 업로드는 S3 호출까지 타므로 자격증명 없는 로컬에선 실패할 수 있어 기본 off.

// k6 run scripts/scenarios/profile-update-load-test.ts
// # 이미지 업로드까지 (S3 자격증명 유효한 환경만): -e WITH_IMAGE=true
// # VU 수 조정: -e TARGET_VUS=30

import { check } from 'k6';
import exec from 'k6/execution';
import config, { endpointThresholds, warmupLoadScenarios } from '../config.ts';
import { updateProfile } from '../api/user.api.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { randomThinkTime, randomInt } from '../utils/random.ts';
import { setupAuthWithProfile } from '../utils/setup.ts';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_VUS = Number(__ENV.WARMUP_VUS || 5);
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';
const WITH_IMAGE = (__ENV.WITH_IMAGE || 'false') === 'true';

export const options = {
  // CSRF 쿠키를 VU당 한 번만 발급받아 재사용하기 위함 (기본값은 반복마다 쿠키 저장소 초기화)
  noCookiesReset: true,
  scenarios: warmupLoadScenarios({
    exec: 'run',
    targetVus: TARGET_VUS,
    rampTime: RAMP_TIME,
    holdTime: HOLD_TIME,
    warmupVus: WARMUP_VUS,
    warmupTime: WARMUP_TIME,
  }),
  thresholds: {
    http_req_failed: ['rate<0.01'],
    ...endpointThresholds(config.tags.user.update, ['p(95)<800'], 'load'),
    ...endpointThresholds(config.tags.auth.csrfToken, ['p(95)<300']),
    ...endpointThresholds(config.tags.auth.signIn, ['p(95)<800']),
  },
};

type SetupData = { token: string; userId: string }[];

export function setup(): SetupData {
  return setupAuthWithProfile(TARGET_VUS);
}

// 1x1 흰색 픽셀 PNG. 실제 이미지 처리 경로(S3 업로드)만 태우기 위한 최소 더미.
const DUMMY_PNG = new Uint8Array([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d,
  0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
  0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53, 0xde, 0x00, 0x00, 0x00,
  0x0c, 0x49, 0x44, 0x41, 0x54, 0x08, 0xd7, 0x63, 0xf8, 0xcf, 0xc0, 0x00,
  0x00, 0x03, 0x01, 0x01, 0x00, 0x18, 0xdd, 0x8d, 0xb0, 0x00, 0x00, 0x00,
  0x00, 0x49, 0x45, 0x4e, 0x44, 0xae, 0x42, 0x60, 0x82,
]).buffer;

// warmup / load 두 시나리오가 공유하는 실행 함수
export function run(data: SetupData): void {
  if (data.length === 0) {
    throw new Error('[VU] 로그인된 계정이 없습니다. setup()을 확인하세요.');
  }
  const account = data[(exec.vu.idInTest - 1) % data.length];

  const request = { name: `부하테스트유저${randomInt(1, 100000)}` };
  const image = WITH_IMAGE
    ? { data: DUMMY_PNG, filename: 'profile.png', contentType: 'image/png' }
    : undefined;

  const body = updateProfile(account.token, account.userId, request, image);

  check(body, {
    '응답 본문 존재': (b) => b !== null,
    '변경한 이름 반영': (b) => b?.name === request.name,
  });

  randomThinkTime(2, 5);
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'profile-update-load-test-summary.html');
}
