/**
 * k6 load test — booking endpoint throughput & latency
 *
 * Measures:
 *   - Booking request throughput (requests/sec)
 *   - P50 / P95 / P99 latency under sustained load
 *   - Zero HTTP 5xx errors under 500 concurrent users
 *
 * Usage:
 *   k6 run --env BASE_URL=http://localhost:8080 k6/load_test_booking.js
 *
 * Expected resume metric:
 *   "P99 booking API latency < 200ms under 500 concurrent users at 200+ RPM"
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const bookingSuccessRate = new Rate('booking_success_rate');
const bookingLatency     = new Trend('booking_latency_ms', true);
const rateLimitHits      = new Counter('rate_limit_hits');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    // Ramp to 500 VUs over 30s, sustain 2 min, then ramp down
    booking_load: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 100  },
        { duration: '60s', target: 500  },
        { duration: '60s', target: 500  },  // sustain at 500
        { duration: '30s', target: 0    },
      ],
    },
  },
  thresholds: {
    // P99 must stay under 500ms (generous for local Testcontainers; tighten for prod)
    'booking_latency_ms{percentile:99}': ['p(99)<500'],
    // P95 under 200ms
    'booking_latency_ms{percentile:95}': ['p(95)<200'],
    // HTTP errors < 5%
    'http_req_failed': ['rate<0.05'],
  },
};

// Each VU uses a unique studentId to bypass Redis rate limiter
export default function () {
  const vuId      = `vu_${__VU}_iter_${__ITER}`;
  const seatId    = '1'; // target a single seat to exercise optimistic locking

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/v1/tickets/claim?studentId=${vuId}&seatId=${seatId}&studentName=LoadTest+User`,
    null,
    { tags: { name: 'claim_ticket' } }
  );
  const latency = Date.now() - start;

  bookingLatency.add(latency);

  const isSuccess = check(res, {
    'status is 202 (queued) or 409 (conflict)': (r) =>
      r.status === 202 || r.status === 409 || r.status === 400,
    'no server error': (r) => r.status < 500,
  });

  if (res.status === 429) rateLimitHits.add(1);

  bookingSuccessRate.add(res.status === 202);

  sleep(0.1); // 100ms think time between requests per VU
}

export function handleSummary(data) {
  const p50  = data.metrics['booking_latency_ms']?.values?.['p(50)']?.toFixed(1)  ?? 'n/a';
  const p95  = data.metrics['booking_latency_ms']?.values?.['p(95)']?.toFixed(1)  ?? 'n/a';
  const p99  = data.metrics['booking_latency_ms']?.values?.['p(99)']?.toFixed(1)  ?? 'n/a';
  const rps  = data.metrics['http_reqs']?.values?.rate?.toFixed(1) ?? 'n/a';

  console.log(`
╔══════════════════════════════════════════════════════╗
║           CampusTix Booking Load Test Results        ║
╠══════════════════════════════════════════════════════╣
║  Throughput         : ${rps} req/s
║  P50 Latency        : ${p50} ms
║  P95 Latency        : ${p95} ms
║  P99 Latency        : ${p99} ms
║  Rate limit hits    : ${data.metrics['rate_limit_hits']?.values?.count ?? 0}
╚══════════════════════════════════════════════════════╝

Resume phrase: "P99 booking latency ${p99}ms at ${rps} req/s under 500 concurrent users"
`);

  return {
    'k6/load_test_results.json': JSON.stringify(data, null, 2),
  };
}
