import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    contract_smoke: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '10s', target: 5 },
        { duration: '20s', target: 10 },
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

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:18080';

export default function () {
  const res = http.get(`${BASE_URL}/payments/success?paymentKey=pay_123&orderId=order_key_123&amount=1000`, {
    tags: { name: 'payment_success_contract' },
    timeout: '3s',
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has orderId': (r) => r.json('data.orderId') === 18,
    'has orderKey': (r) => r.json('data.orderKey') === 'order_key_123',
    'has succeeded status': (r) => r.json('data.status') === 'SUCCEEDED',
  });

  sleep(1);
}
