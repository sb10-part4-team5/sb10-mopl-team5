import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';

import { loginByIndexWithProfile } from '../api/auth.api.ts';
import { getUser } from '../api/user.api.ts';
import config from '../config.ts';
import type { UserResponse } from '../types/user.type.ts';
import { summaryHandler } from '../utils/reporter.ts';

const DURATION = __ENV.DURATION || '1m';
const VUS = Number(__ENV.VUS || 10);
const TEST_PHASE = __ENV.TEST_PHASE || 'main';
const RESULT_DIR = __ENV.RESULT_DIR || 'results';

const userDetailSuccessRate = new Rate(
    'user_detail_success_rate',
);

interface UserSession {
  token: string;
  userId: string;
}

interface SetupData {
  sessions: UserSession[];
}

export const options = {
  scenarios: {
    user_detail: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
      gracefulStop: '10s',
    },
  },

  thresholds: {
    user_detail_success_rate: [
      'rate>0.99',
    ],

    [`http_req_duration{name:${config.tags.user.detail}}`]: [
      'p(95)<500',
    ],

    [`http_req_failed{name:${config.tags.user.detail}}`]: [
      'rate<0.01',
    ],
  },
};

export function setup(): SetupData {
  const sessions: UserSession[] = [];

  for (let index = 1; index <= VUS; index += 1) {
    sessions.push(loginByIndexWithProfile(index));
  }

  return { sessions };
}

export default function (data: SetupData): void {
  const vuIndex = exec.vu.idInTest - 1;
  const session = data.sessions[vuIndex];

  if (!session) {
    throw new Error(
        `VU에 대응하는 로그인 세션이 없습니다. vuIndex=${vuIndex + 1}`,
    );
  }

  const response = getUser(
      session.token,
      session.userId,
      config.tags.user.detail,
  );

  const succeeded = check(response, {
    '사용자 상세 조회 성공': (
        value: UserResponse | null,
    ): boolean => value !== null,

    '사용자 ID 일치': (
        value: UserResponse | null,
    ): boolean => value?.id === session.userId,

    '이메일 존재': (
        value: UserResponse | null,
    ): boolean => Boolean(value?.email),

    '권한 존재': (
        value: UserResponse | null,
    ): boolean => Boolean(value?.role),

    '잠금 상태 존재': (
        value: UserResponse | null,
    ): boolean => typeof value?.locked === 'boolean',
  });

  userDetailSuccessRate.add(succeeded);

  sleep(Math.random() * 2 + 1);
}

export function handleSummary(data: unknown) {
  if (TEST_PHASE === 'warmup') {
    return {};
  }

  return summaryHandler(
      data,
      `${RESULT_DIR}/summary.html`,
  );
}
