import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '10s', target: 1000 },   // Ramp up to 1000 VUs over 10s
    { duration: '20s', target: 10000 },  // Ramp up to 10000 VUs over 20s
    { duration: '30s', target: 10000 },  // Hold at 10000 VUs for 30s
  ],
  thresholds: {
    // Allow 409 (SOLD_OUT) as expected behavior - only fail if >99% requests fail with unexpected errors
    http_req_failed: ['rate<0.99'],
    // Track custom metrics for analysis
    'checks': ['rate>0.99'],
  },
  // Graceful stop - allow pending requests to complete
  gracefulStop: '10s',
};

// Default product ID - update this after checking your seeded product
const PRODUCT_ID = '00000000-0000-0000-0000-000000000001';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const userId = uuidv4();
  
  const payload = JSON.stringify({
    productId: PRODUCT_ID,
    userId: userId,
    quantity: 1,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(`${BASE_URL}/api/purchase`, payload, params);

  // Check response: 200 = successful reservation, 409 = sold out (expected)
  check(res, {
    'status is 200 (success) or 409 (sold out)': (r) => r.status === 200 || r.status === 409,
    'response has body': (r) => r.body && r.body.length > 0,
  });

  // Log successful purchases for verification
  if (res.status === 200) {
    console.log(`SUCCESS: User ${userId} got reservation`);
  }

  // Small sleep to prevent overwhelming the server (0.1 - 0.3 seconds)
  sleep(Math.random() * 0.2 + 0.1);
}

export function handleSummary(data) {
  // Custom summary output
  const totalRequests = data.metrics.http_reqs.values.count;
  const successRate = data.metrics.checks ? data.metrics.checks.values.rate : 0;
  
  console.log('\n========== FLASH SALE LOAD TEST SUMMARY ==========');
  console.log(`Total Requests: ${totalRequests}`);
  console.log(`Success Rate: ${(successRate * 100).toFixed(2)}%`);
  console.log(`Avg Response Time: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`);
  console.log(`p95 Response Time: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
  console.log(`p99 Response Time: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms`);
  console.log('==================================================\n');

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

// Import text summary for console output
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
