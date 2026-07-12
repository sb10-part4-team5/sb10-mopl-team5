// k6 진입점(entry point)
//
// 공용 인증 흐름의 표준 형태를 보여주는 스모크 테스트다.
//   setup(): VU 수만큼 계정을 미리 로그인해 토큰 배열을 만든다 (테스트 시작 전 1회).
//   default(): 각 VU가 exec.vu.idInTest 로 "자기 몫의 토큰"만 사용해 조회한다.
//
// 왜 이렇게 하나:
//   - 콘텐츠 조회 API 는 authenticated() 라 로그인 필요.
//   - "계정당 세션 1개" 정책 때문에 여러 VU가 같은 계정을 쓰면 서로 세션을 무효화 → 401.
//     따라서 VU마다 별도 계정(user{i}@loadtest.local)을 고정 매핑한다.
//   - 로그인을 setup 에서 미리 끝내므로, 본 측정 중엔 로그인 지연이 섞이지 않는다.
//
// 실제 조회/쓰기 부하 시나리오는 scripts/scenarios/ 에 추가하고 여기서 등록한다.

import { check } from 'k6';
import exec from 'k6/execution';
import config, { commonThresholds, ContentSortBy, SortDirection } from './config.ts';
import { get } from './utils/http-client.ts';
import { summaryHandler } from './utils/reporter.ts';
import { loginByIndex } from './api/auth.api.ts';
import { CursorResponse, ContentResponse } from './types/content.type.ts';

// 스모크 기본 VU 수 (실제 부하 단계에서는 시나리오별 options 로 override)
const VUS = Number(__ENV.VUS || 5);

export const options = {
  vus: VUS,
  iterations: VUS, // 스모크: VU당 1회
  thresholds: commonThresholds,
};

type SetupData = { token: string }[];

// 테스트 시작 전 1회: VU 수만큼 계정(user1..userN)을 로그인해 토큰을 확보한다.
export function setup(): SetupData {
  const tokens: SetupData = [];
  for (let i = 1; i <= VUS; i++) {
    tokens.push({ token: loginByIndex(i) });
  }
  console.log(`[setup] ${tokens.length}개 계정 로그인 완료`);
  return tokens;
}

// 각 VU는 자기 인덱스에 해당하는 토큰만 사용 (계정 공유 → 세션 충돌 방지)
export default function (data: SetupData): void {
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
