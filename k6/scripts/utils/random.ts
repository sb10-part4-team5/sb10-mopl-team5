import { sleep } from 'k6';

// 실제 사용자처럼 요청 사이에 임의의 대기(think time)를 준다. 단위: 초
export function randomThinkTime(minSec: number, maxSec: number): void {
  const t = Math.random() * (maxSec - minSec) + minSec;
  sleep(t);
}

// min ~ max 사이의 정수 (양끝 포함)
export function randomInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// 배열에서 임의의 원소 하나
export function pickOne<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)];
}
