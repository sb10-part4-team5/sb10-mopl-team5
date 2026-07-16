// handleSummary 용 HTML 리포트 생성기.
// 사용: export function handleSummary(data) { return summaryHandler(data); }
// ⚠️ textSummary 는 원격 jslib import → 최초 실행 시 인터넷 필요 (k6가 캐시).

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

function fmt(v: number | undefined, digits = 2): string {
  return (v ?? 0).toFixed(digits);
}

// 엔드포인트별(태그 name:...) 지표 추출 (해당 서브메트릭/thresholds 있을 때만)
function extractPerEndpoint(metrics: Record<string, any>) {
  const rows: {
    name: string; count: number; avg: number; p95: number; max: number; errorRate: number | null;
  }[] = [];

  for (const [key, value] of Object.entries(metrics)) {
    const braced = key.match(/^http_req_duration\{(.+)\}$/);
    if (!braced) continue;
    const nameMatch = braced[1].match(/name:([^,}]+)/);
    if (!nameMatch) continue;

    const dur = value?.values ?? {};
    const reqKey = key.replace('http_req_duration', 'http_reqs');
    const failKey = key.replace('http_req_duration', 'http_req_failed');

    rows.push({
      name: nameMatch[1],
      count: metrics[reqKey]?.values?.count ?? 0,
      avg: dur.avg ?? 0,
      p95: dur['p(95)'] ?? 0,
      max: dur.max ?? 0,
      errorRate: metrics[failKey] ? (metrics[failKey].values?.rate ?? 0) * 100 : null,
    });
  }
  return rows.sort((a, b) => b.count - a.count);
}

export function generateReport(data: any): string {
  const metrics = data?.metrics ?? {};
  const dur = metrics.http_req_duration?.values ?? {};
  const reqs = metrics.http_reqs?.values ?? {};
  const failed = metrics.http_req_failed?.values ?? {};
  const checks = metrics.checks?.values ?? {};
  const iters = metrics.iterations?.values ?? {};
  const vusMax = metrics.vus_max?.values?.value ?? metrics.vus_max?.values?.max ?? 0;

  // sse_connection_failed Rate 메트릭이 있으면 연결 실패율로 에러율 대체
  // (SSE는 타임아웃이 정상 종료라 http_req_failed 가 의미없음)
  const sseFailedValues = metrics['sse_connection_failed']?.values;
  const errorRate = sseFailedValues
    ? (sseFailedValues.rate ?? 0) * 100
    : (failed.rate ?? 0) * 100;

  const errorLabel = sseFailedValues ? 'SSE 실패율' : '에러율';
  const checksRate = (checks.rate ?? 0) * 100;
  const sseEventRate = metrics['sse_connect_event_received']?.values?.rate;
  const perEndpoint = extractPerEndpoint(metrics);

  const endpointRows = perEndpoint.length
    ? perEndpoint.map((r) => `
        <tr>
          <td class="left">${r.name}</td>
          <td>${r.count}</td>
          <td>${fmt(r.avg)}</td>
          <td>${fmt(r.p95)}</td>
          <td>${fmt(r.max)}</td>
          <td class="${r.errorRate === null ? '' : r.errorRate > 0 ? 'bad' : 'ok'}">${r.errorRate === null ? '—' : fmt(r.errorRate) + '%'}</td>
        </tr>`).join('')
    : `<tr><td colspan="6" class="muted">엔드포인트별 지표가 없습니다.
         시나리오에서 요청에 tag(name)를 달고 thresholds(예: <code>http_req_duration{name:...}</code>)를
         정의하면 여기에 표시됩니다.</td></tr>`;

  return `<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>MOPL 부하테스트 리포트</title>
<style>
 body{font-family:'Segoe UI',Tahoma,sans-serif;background:#f4f7f6;color:#2c3e50;padding:24px;margin:0}
 h1{text-align:center}
 .cards{display:flex;flex-wrap:wrap;gap:16px;justify-content:center;margin:20px 0}
 .card{background:#fff;border-radius:10px;box-shadow:0 2px 6px rgba(0,0,0,.06);padding:16px 22px;min-width:140px;text-align:center}
 .card .label{font-size:.82em;color:#7f8c8d}
 .card .value{font-size:1.5em;font-weight:bold;margin-top:6px}
 table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,.06);margin-top:8px}
 th,td{padding:11px 14px;border-bottom:1px solid #eef2f5;text-align:center}
 th{background:#34495e;color:#fff}
 td.left{text-align:left}
 .bad{color:#e74c3c;font-weight:bold}
 .ok{color:#27ae60;font-weight:bold}
 .muted{color:#95a5a6}
 code{background:#eef2f5;padding:1px 5px;border-radius:4px}
 .ts{text-align:center;color:#95a5a6;margin-top:20px;font-size:.85em}
</style>
</head>
<body>
 <h1>🚀 MOPL 부하테스트 리포트</h1>

 <div class="cards">
  <div class="card"><div class="label">최대 VU</div><div class="value">${vusMax}</div></div>
  <div class="card"><div class="label">총 요청 수</div><div class="value">${reqs.count ?? 0}</div></div>
  <div class="card"><div class="label">RPS</div><div class="value">${fmt(reqs.rate)}</div></div>
  <div class="card"><div class="label">${errorLabel}</div><div class="value ${errorRate > 0 ? 'bad' : 'ok'}">${fmt(errorRate)}%</div></div> 
  <div class="card"><div class="label">체크 성공률</div>
  <div class="value ${checksRate < 100 ? 'bad' : 'ok'}">${fmt(checksRate)}%</div></div>
   ${sseEventRate !== undefined ? `<div class="card"><div class="label">connect 이벤트 수신률</div><div class="value ${sseEventRate < 0.99 ? 'bad' : 'ok'}">${fmt(sseEventRate * 100)}%</div></div>` : ''}
 </div>

 <div class="cards">
   <div class="card"><div class="label">평균 응답</div><div class="value">${fmt(dur.avg)} ms</div></div>
   <div class="card"><div class="label">p90</div><div class="value">${fmt(dur['p(90)'])} ms</div></div>
   <div class="card"><div class="label">p95</div><div class="value">${fmt(dur['p(95)'])} ms</div></div>
   <div class="card"><div class="label">최대</div><div class="value">${fmt(dur.max)} ms</div></div>
   <div class="card"><div class="label">반복(iterations)</div><div class="value">${iters.count ?? 0}</div></div>
 </div>

 <table>
   <tr><th class="left">엔드포인트</th><th>요청 수</th><th>평균(ms)</th><th>p95(ms)</th><th>최대(ms)</th><th>에러율</th></tr>
   ${endpointRows}
 </table>

 <p class="ts">생성 시각: ${new Date().toISOString()}</p>
</body>
</html>`;
}

// 파일명의 확장자 앞에 타임스탬프(KST)를 끼워 넣어 실행마다 파일이 겹치지 않게 함
// (예: content-read-latest-summary.html -> content-read-latest-summary-2026-07-13T17-07-54-471.html)
// toISOString()은 항상 UTC라 +9시간 보정 후 'Z'를 뗀다 (그대로 두면 UTC라고 오해하기 쉬움).
function withTimestamp(basePath: string): string {
  const KST_OFFSET_MS = 9 * 60 * 60 * 1000;
  const suffix = new Date(Date.now() + KST_OFFSET_MS)
    .toISOString()
    .replace('Z', '')
    .replace(/[:.]/g, '-');
  const dotIndex = basePath.lastIndexOf('.');
  if (dotIndex === -1) return `${basePath}-${suffix}`;
  return `${basePath.slice(0, dotIndex)}-${suffix}${basePath.slice(dotIndex)}`;
}

// 터미널 요약 + HTML 파일 동시 출력. 파일명은 실행마다 자동으로 고유해짐 (덮어쓰기 방지)
export function summaryHandler(data: any, htmlPath = 'summary.html') {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    [withTimestamp(htmlPath)]: generateReport(data),
  };
}
