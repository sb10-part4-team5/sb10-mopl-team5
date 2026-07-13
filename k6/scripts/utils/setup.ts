import { loginByIndex } from '../api/auth.api.ts';

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
