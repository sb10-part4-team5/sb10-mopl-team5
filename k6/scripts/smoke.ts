// 스모크 테스트 (연결 점검 + 공용 패턴 예시). 실제 부하는 scripts/scenarios/ 에서.
//   setup(): VU 수만큼 미리 로그인 (본 측정에 로그인 지연 안 섞이게)
//   default(): VU마다 자기 계정 토큰만 사용 → "계정당 세션 1개" 정책 충돌(401) 방지

import { check } from 'k6';
import exec from 'k6/execution';
import config, { commonThresholds, ContentSortBy, SortDirection } from './config.ts';
import { get } from './utils/http-client.ts';
import { summaryHandler } from './utils/reporter.ts';
import { CursorResponse, ContentResponse } from './types/content.type.ts';
import { setupAuth } from "./utils/setup.ts";

const VUS = Number(__ENV.VUS || 5);

export const options = {
  // per-vu-iterations: VU마다 정확히 iterations 회 실행을 "보장" (shared-iterations는 공유 풀이라 미보장)
  // VU별 고정 계정 매핑(exec.vu.idInTest) 설계상, 모든 VU가 실행되는 게 보장돼야 함
  scenarios: {
    smoke: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '30s', // 안전장치: 이 시간 넘으면 강제 종료
    },
  },
  thresholds: commonThresholds,
};

type SetupData = string[];

// 시작 전 1회: VU 수만큼 계정 로그인해 토큰 확보
export function setup(): SetupData {
  return setupAuth(VUS);
}

// VU마다 자기 인덱스 토큰만 사용
export default function (data: SetupData): void {
  // 빈 배열이면 % 0 → NaN → undefined.token 이므로 명확히 중단
  if (data.length === 0) {
    throw new Error('[VU] 사용 가능한 계정 토큰이 없습니다. setup() 로그인 실패 또는 VUS=0 여부를 확인하세요.');
  }
  const token = data[(exec.vu.idInTest - 1) % data.length];

  const params = `limit=10&sortDirection=${SortDirection.DESC}&sortBy=${ContentSortBy.CREATED_AT}`;
  const url = `${config.endpoints.content.list}?${params}`;

  const body = get<CursorResponse<ContentResponse>>(url, {
    token: token,
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
