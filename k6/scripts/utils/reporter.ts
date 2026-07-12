// k6 handleSummary 용 커스텀 HTML 리포트 생성기 (공용)
//
// 각 시나리오 파일에서 이렇게 재사용:
//   import { summaryHandler } from '../utils/reporter.ts';
//   export function handleSummary(data: any) {
//     return summaryHandler(data);            // stdout 요약 + summary.html 동시 생성
//   }
//
// ⚠️ textSummary 는 원격 jslib 에서 import 하므로, 최초 실행 시 인터넷 연결이 필요합니다
//    (k6가 받아서 캐시함).

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

function fmt(v: number | undefined, digits = 2): string {
  return (v ?? 0).toFixed(digits);
}

// 엔드포인트별(태그 name:...) 지표 추출. 시나리오에서 해당 서브메트릭/thresholds 가 있을 때만 채워짐.
function extractPerEndpoint(metrics: Record<string, any>) {
  const rows: {
    name: string; count: number; avg: number; p95: number; max: number; errorRate: number;
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
      errorRate: (metrics[failKey]?.values?.rate ?? 0) * 100,
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

  const errorRate = (failed.rate ?? 0) * 100;
  const checksRate = (checks.rate ?? 0) * 100;
  const perEndpoint = extractPerEndpoint(metrics);

  const endpointRows = perEndpoint.length
    ? perEndpoint.map((r) => `
        <tr>
          <td class="left">${r.name}</td>
          <td>${r.count}</td>
          <td>${fmt(r.avg)}</td>
          <td>${fmt(r.p95)}</td>
          <td>${fmt(r.max)}</td>
          <td class="${r.errorRate > 0 ? 'bad' : 'ok'}">${fmt(r.errorRate)}%</td>
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
   <div class="card"><div class="label">에러율</div><div class="value ${errorRate > 0 ? 'bad' : 'ok'}">${fmt(errorRate)}%</div></div>
   <div class="card"><div class="label">체크 성공률</div><div class="value ${checksRate < 100 ? 'bad' : 'ok'}">${fmt(checksRate)}%</div></div>
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

// handleSummary 에서 그대로 반환할 수 있는 형태 (터미널 요약 + HTML 파일 동시 출력)
export function summaryHandler(data: any, htmlPath = 'summary.html') {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    [htmlPath]: generateReport(data),
  };
}
