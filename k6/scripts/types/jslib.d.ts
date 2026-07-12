// k6 원격 jslib 모듈 타입 선언 (TS가 https import를 인식하도록)
declare module 'https://jslib.k6.io/k6-summary/0.0.1/index.js' {
  export function textSummary(
    data: unknown,
    options?: { indent?: string; enableColors?: boolean },
  ): string;
}
