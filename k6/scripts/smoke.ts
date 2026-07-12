// 스모크 테스트 (연결 점검 + 공용 패턴 예시). 실제 부하는 scripts/scenarios/ 에서.
//   setup(): VU 수만큼 미리 로그인 (본 측정에 로그인 지연 안 섞이게)
//   default(): VU마다 자기 계정 토큰만 사용 → "계정당 세션 1개" 정책 충돌(401) 방지

import { check } from 'k6';
import exec from 'k6/execution';
import config, { commonThresholds, ContentSortBy, SortDirection } from './config.ts';
import { get } from './utils/http-client.ts';
import { summaryHandler } from './utils/reporter.ts';
import { loginByIndex } from './api/auth.api.ts';
import { CursorResponse, ContentResponse } from './types/content.type.ts';

const VUS = Number(__ENV.VUS || 5);

export const options = {
  vus: VUS,
  iterations: VUS, // 스모크: VU당 1회
  thresholds: commonThresholds,
};

type SetupData = { token: string }[];

// 시작 전 1회: VU 수만큼 계정 로그인해 토큰 확보
export function setup(): SetupData {
  const tokens: SetupData = [];
  for (let i = 1; i <= VUS; i++) {
    tokens.push({ token: loginByIndex(i) });
  }
  // 빈 배열 방지 (아래 % 0 → NaN 가드)
  if (tokens.length === 0) {
    throw new Error(`[setup] 로그인된 계정이 없습니다 (VUS=${VUS}). VUS>=1 인지, 서버·계정 시딩을 확인하세요.`);
  }
  console.log(`[setup] ${tokens.length}개 계정 로그인 완료`);
  return tokens;
}

// VU마다 자기 인덱스 토큰만 사용
export default function (data: SetupData): void {
  // 빈 배열이면 % 0 → NaN → undefined.token 이므로 명확히 중단
  if (data.length === 0) {
    throw new Error('[VU] 사용 가능한 계정 토큰이 없습니다. setup() 로그인 실패 또는 VUS=0 여부를 확인하세요.');
  }
  const account = data[(exec.vu.idInTest - 1) % data.length];

  const params = `limit=10&sortDirection=${SortDirection.DESC}&sortBy=${ContentSortBy.CREATED_AT}`;
  const url = `${config.endpoints.content.list}?${params}`;

  const body = get<CursorResponse<ContentResponse>>(url, {
    token: account.token,
    tag: config.tags.content.list,
  });

  check(body, {
    '응답 본문 존재': (b) => b !== null,
    'data 배열 존재': (b) => Array.isArray(b?.data),
  });
}

// 테스트 종료 시: 터미널 요약 + summary.html 생성
export function handleSummary(data: any) {
  return summaryHandler(data);
}
