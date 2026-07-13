import { loginByIndex } from '../api/auth.api.ts';

/**
 * 지정한 VU 수만큼 1번부터 순차적으로 로그인하여 토큰 배열을 반환합니다.
 */
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
