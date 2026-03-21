import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    state_transition_mix: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '10s', target: 5 },
        { duration: '20s', target: 12 },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:18081';
const paths = [
  '/payment-state/success',
  '/payment-state/retryable-fail',
  '/payment-state/nonretryable-fail',
];

export default function () {
  const path = paths[__ITER % paths.length];
  const res = http.get(`${BASE_URL}${path}`, {
    tags: { name: 'payment_state_transition' },
    timeout: '3s',
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'status field exists': (r) => !!r.json('data.status'),
    'success keeps stock': (r) => path !== '/payment-state/success' || r.json('data.stockReleased') === false,
    'retryable keeps stock': (r) => path !== '/payment-state/retryable-fail' || r.json('data.stockReleased') === false,
    'nonretryable releases stock': (r) => path !== '/payment-state/nonretryable-fail' || r.json('data.stockReleased') === true,
  });

  sleep(1);
}
