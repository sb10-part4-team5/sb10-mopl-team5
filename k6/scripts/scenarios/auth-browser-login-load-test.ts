import { check } from 'k6';
import exec from 'k6/execution';
import config, { commonThresholds } from '../config.ts';
import { loginByIndex } from '../api/auth.api.ts';
import { summaryHandler } from '../utils/reporter.ts';

const VUS = Number(__ENV.VUS || 10);

export const options = {
  scenarios: {
    auth_browser_login: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  thresholds: {
    ...commonThresholds,
    [`http_req_duration{name:${config.tags.auth.csrfToken}}`]: [
      'p(95)<300',
    ],
    [`http_reqs{name:${config.tags.auth.csrfToken}}`]: [
      'count>0',
    ],
    [`http_req_failed{name:${config.tags.auth.csrfToken}}`]: [
      'rate<0.01',
    ],

    [`http_req_duration{name:${config.tags.auth.signIn}}`]: [
      'p(95)<500',
    ],
    [`http_reqs{name:${config.tags.auth.signIn}}`]: [
      'count>0',
    ],
    [`http_req_failed{name:${config.tags.auth.signIn}}`]: [
      'rate<0.01',
    ],
  },
};

export default function (): void {
  const accountIndex = exec.vu.idInTest;
  const accessToken = loginByIndex(accountIndex);

  check(accessToken, {
    'accessToken 발급 성공': (token) =>
        typeof token === 'string' && token.length > 0,
  });
}

export function handleSummary(data: any) {
  return summaryHandler(data, 'summary-auth-browser-login.html');
}
