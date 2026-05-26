import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '5s', target: 50 },    // Ramp up to 50 VUs
    { duration: '10s', target: 100 },  // Ramp up to 100 VUs
    { duration: '15s', target: 100 },  // Hold at 100 VUs
  ],
  thresholds: {
    http_req_failed: ['rate<0.5'],  // Allow some failures (sold out scenarios)
    'checks': ['rate>0.8'],
  },
  gracefulStop: '5s',
};

const PRODUCT_ID = '00000000-0000-0000-0000-000000000001';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const userId = uuidv4();
  
  // Step 1: Make a purchase request to get a reservation
  const purchasePayload = JSON.stringify({
    productId: PRODUCT_ID,
    userId: userId,
    quantity: 1,
  });

  const purchaseParams = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const purchaseRes = http.post(`${BASE_URL}/api/purchase`, purchasePayload, purchaseParams);

  // Check if purchase was successful
  const purchaseSuccess = check(purchaseRes, {
    'purchase status is 200': (r) => r.status === 200,
  });

  // If purchase failed (sold out), skip confirmation
  if (!purchaseSuccess || purchaseRes.status !== 200) {
    if (purchaseRes.status === 409) {
      console.log(`User ${userId}: Product sold out, skipping confirmation`);
    } else {
      console.log(`User ${userId}: Purchase failed with status ${purchaseRes.status}`);
    }
    sleep(0.5);
    return;
  }

  // Step 2: Extract reservation token from purchase response
  let purchaseData;
  try {
    purchaseData = JSON.parse(purchaseRes.body);
  } catch (e) {
    console.log(`User ${userId}: Failed to parse purchase response`);
    return;
  }

  const reservationToken = purchaseData.reservationToken;
  
  if (!reservationToken) {
    console.log(`User ${userId}: No reservation token in response`);
    return;
  }

  console.log(`User ${userId}: Got reservation token ${reservationToken}`);

  // Small delay before confirmation (simulates user reviewing order)
  sleep(Math.random() * 0.5 + 0.2);

  // Step 3: Confirm the order with the reservation token
  const confirmPayload = JSON.stringify({
    reservationToken: reservationToken,
  });

  const confirmParams = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const confirmRes = http.post(`${BASE_URL}/api/confirm-order`, confirmPayload, confirmParams);

  // Check confirmation response
  check(confirmRes, {
    'confirm status is 200': (r) => r.status === 200,
    'confirm response has orderId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.orderId !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (confirmRes.status === 200) {
    try {
      const confirmData = JSON.parse(confirmRes.body);
      console.log(`SUCCESS: User ${userId} confirmed order ${confirmData.orderId}`);
    } catch {
      console.log(`SUCCESS: User ${userId} confirmed order`);
    }
  } else {
    console.log(`User ${userId}: Order confirmation failed with status ${confirmRes.status}`);
  }

  // Small sleep between iterations
  sleep(Math.random() * 0.3 + 0.1);
}

export function handleSummary(data) {
  console.log('\n========== ORDER CONFIRMATION FLOW TEST SUMMARY ==========');
  console.log(`Total Requests: ${data.metrics.http_reqs.values.count}`);
  console.log(`Success Rate: ${(data.metrics.checks.values.rate * 100).toFixed(2)}%`);
  console.log(`Avg Response Time: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`);
  console.log(`p95 Response Time: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`);
  console.log('============================================================\n');

  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
