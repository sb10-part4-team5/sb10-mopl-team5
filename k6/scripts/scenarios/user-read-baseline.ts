// 관리자 사용자 목록 조회 부하테스트 — 단일 CASE 격리 실행 (+ 워밍업)
// CASE=first-page: 검색 조건 없는 첫 페이지
// CASE=email-existing: 각 VU 계정의 전체 이메일을 emailLike로 검색

import { check } from 'k6';
import exec from 'k6/execution';

import config, {
  endpointThresholds,
  warmupLoadScenarios,
} from '../config.ts';
import { getUsers } from '../api/user.api.ts';
import type { UserListParams } from '../api/user.api.ts';
import type { CursorResponse } from '../types/global.type.ts';
import type { UserResponse } from '../types/user.type.ts';
import { randomThinkTime } from '../utils/random.ts';
import { summaryHandler } from '../utils/reporter.ts';
import { setupAuth } from '../utils/setup.ts';

type UserReadCase = 'first-page' | 'email-existing';

const CASE_NAMES: UserReadCase[] = [
  'first-page',
  'email-existing',
];

const CASE = (__ENV.CASE || 'first-page') as UserReadCase;

if (!CASE_NAMES.includes(CASE)) {
  throw new Error(
      `[config] 알 수 없는 CASE="${CASE}". 가능한 값: ${CASE_NAMES.join(', ')}`,
  );
}

const TARGET_VUS = Number(__ENV.TARGET_VUS || 20);
const RAMP_TIME = __ENV.RAMP_TIME || '30s';
const HOLD_TIME = __ENV.HOLD_TIME || '1m';
const WARMUP_VUS = Number(
    __ENV.WARMUP_VUS || Math.min(5, TARGET_VUS),
);
const WARMUP_TIME = __ENV.WARMUP_TIME || '20s';
const LIMIT = 20;

if (!Number.isInteger(TARGET_VUS) || TARGET_VUS < 1) {
  throw new Error(
      `[config] TARGET_VUS는 1 이상의 정수여야 합니다: ${__ENV.TARGET_VUS}`,
  );
}

if (!Number.isInteger(WARMUP_VUS) || WARMUP_VUS < 1) {
  throw new Error(
      `[config] WARMUP_VUS는 1 이상의 정수여야 합니다: ${__ENV.WARMUP_VUS}`,
  );
}

if (WARMUP_VUS > TARGET_VUS) {
  throw new Error(
      `[config] WARMUP_VUS는 TARGET_VUS 이하여야 합니다: `
      + `${WARMUP_VUS} > ${TARGET_VUS}`,
  );
}

const TAG = CASE === 'first-page'
    ? config.tags.user.firstPage
    : config.tags.user.emailExisting;

const loadFailureMetric =
    `http_req_failed{name:${TAG},scenario:load}`;

export const options = {
  scenarios: warmupLoadScenarios({
    exec: 'run',
    targetVus: TARGET_VUS,
    rampTime: RAMP_TIME,
    holdTime: HOLD_TIME,
    warmupVus: WARMUP_VUS,
    warmupTime: WARMUP_TIME,
  }),
  thresholds: {
    ...endpointThresholds(TAG, ['p(95)<500'], 'load'),
    [loadFailureMetric]: ['rate<0.01'],
    'checks{scenario:load}': ['rate==1'],
  },
};

type SetupData = string[];

export function setup(): SetupData {
  return setupAuth(TARGET_VUS);
}

function buildParams(email?: string): UserListParams {
  return {
    limit: LIMIT,
    sortBy: 'createdAt',
    sortDirection: 'DESC',
    ...(email ? { emailLike: email } : {}),
  };
}

function hasRepresentativeFields(
    body: CursorResponse<UserResponse> | null,
): boolean {
  if (!body || body.data.length === 0) {
    return true;
  }

  const first = body.data[0];

  return typeof first.id === 'string'
      && typeof first.email === 'string'
      && typeof first.role === 'string';
}

export function run(data: SetupData): void {
  if (data.length === 0) {
    throw new Error(
        '[VU] 사용 가능한 계정 토큰이 없습니다. '
        + 'setup() 로그인 실패 여부를 확인하세요.',
    );
  }

  const accountIndex =
      ((exec.vu.idInTest - 1) % data.length) + 1;

  const token = data[accountIndex - 1];
  const email = config.loadTestAccount.email(accountIndex);

  const params = buildParams(
      CASE === 'email-existing' ? email : undefined,
  );

  const body = getUsers(token, params, TAG);

  check(body, {
    [`${CASE}: 응답 JSON 파싱 성공`]:
        (b) => b !== null,

    [`${CASE}: data 배열 존재`]:
        (b) => Array.isArray(b?.data),

    [`${CASE}: data 크기 limit 이하`]:
        (b) => Array.isArray(b?.data)
            && b.data.length <= LIMIT,

    [`${CASE}: totalCount 유효`]:
        (b) => typeof b?.totalCount === 'number'
            && Array.isArray(b?.data)
            && b.totalCount >= b.data.length,

    [`${CASE}: createdAt 정렬`]:
        (b) => b?.sortBy === 'createdAt',

    [`${CASE}: 내림차순 응답`]:
        (b) => b?.sortDirection === 'DESCENDING',

    [`${CASE}: hasNext 필드 존재`]:
        (b) => typeof b?.hasNext === 'boolean',

    [`${CASE}: 대표 사용자 필드 존재`]:
        (b) => hasRepresentativeFields(b),
  });

  if (CASE === 'first-page') {
    check(body, {
      'first-page: 결과 존재':
          (b) => Array.isArray(b?.data)
              && b.data.length > 0,

      // 약 1,000명 데이터에서 limit=20인 첫 페이지이므로
      // 다음 페이지가 존재해야 한다.
      'first-page: 다음 페이지 존재':
          (b) => b?.hasNext === true,

      'first-page: 대표 사용자 ADMIN':
          (b) => b?.data?.[0]?.role === 'ADMIN',
    });
  } else {
    check(body, {
      'email-existing: 결과 존재':
          (b) => Array.isArray(b?.data)
              && b.data.length > 0,

      // 이메일은 DB unique이고 전체 이메일 문자열을 전달하므로
      // 현재 시딩 데이터에서는 한 건이 조회된다.
      'email-existing: 결과 한 건':
          (b) => b?.data?.length === 1,

      'email-existing: 검색 이메일 일치':
          (b) => b?.data?.[0]?.email === email,

      'email-existing: 대표 사용자 ADMIN':
          (b) => b?.data?.[0]?.role === 'ADMIN',
    });
  }

  randomThinkTime(1, 3);
}

export function handleSummary(data: unknown) {
  return summaryHandler(
      data,
      `user-read-${CASE}-summary.html`,
  );
}
