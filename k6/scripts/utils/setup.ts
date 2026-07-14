import { loginByIndex, loginByIndexWithProfile } from '../api/auth.api.ts';

// 1번부터 targetVus 개 계정을 순차 로그인해 토큰 배열 반환
export function setupAuth(targetVus: number): string[] {
  const tokens: string[] = [];
  for (let i = 1; i <= targetVus; i++) {
    tokens.push(loginByIndex(i));
  }
  if (tokens.length === 0) {
    throw new Error(`[setup] 로그인된 계정이 없습니다 (targetVus=${targetVus}). 서버·계정 시딩을 확인하세요.`);
  }
  console.log(`[setup] ${tokens.length}개 계정 로그인 완료`);
  return tokens;
}

// 1번부터 targetVus 개 계정을 순차 로그인해 토큰+userId 배열 반환
// Follow/DM처럼 "다른 시딩 계정의 실제 UUID"가 필요한 시나리오에서, 관리자 전용 유저 목록
// 조회 없이 VU 계정들끼리 서로를 대상으로 삼기 위해 씀
// 주의: 순차 동기 로그인이라 targetVus가 매우 커지면(수백 이상) k6 setup() 기본 타임아웃(60초)에
// 걸릴 수 있다. 그 규모까지 갈 땐 http.batch()로 CSRF 발급/로그인을 단계별 일괄 처리하도록
// 바꿔야 한다 (지금 규모에서는 문제없어 보류).
export function setupAuthWithProfile(targetVus: number): { token: string; userId: string }[] {
  const accounts: { token: string; userId: string }[] = [];
  for (let i = 1; i <= targetVus; i++) {
    accounts.push(loginByIndexWithProfile(i));
  }
  if (accounts.length === 0) {
    throw new Error(`[setup] 로그인된 계정이 없습니다 (targetVus=${targetVus}). 서버·계정 시딩을 확인하세요.`);
  }
  console.log(`[setup] ${accounts.length}개 계정 로그인 완료 (userId 포함)`);
  return accounts;
}
