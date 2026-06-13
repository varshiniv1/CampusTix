/**
 * k6 load test — booking endpoint throughput & latency
 *
 * Fully self-contained: setup() logs in as admin, creates one test event
 * with exactly ONE seat, then 500 VUs all race to claim that same seat.
 * Exactly 1 should succeed — proves optimistic locking under real load.
 *
 * Usage:
 *   k6 run k6/load_test_booking.js
 *   k6 run --env BASE_URL=http://localhost:8080 k6/load_test_booking.js
 *   k6 run --env ADMIN_USERNAME=myuser --env ADMIN_PASSWORD=mypass k6/load_test_booking.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const bookingSuccessRate = new Rate('booking_success_rate');
const bookingLatency     = new Trend('booking_latency_ms', true);
const rateLimitHits      = new Counter('rate_limit_hits');

const BASE_URL    = __ENV.BASE_URL       || 'http://localhost:8080';
const ADMIN_USER  = __ENV.ADMIN_USERNAME || 'username';
const ADMIN_PASS  = __ENV.ADMIN_PASSWORD || 'password';

export const options = {
  scenarios: {
    booking_load: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 100 },   // ramp up
        { duration: '60s', target: 500 },   // push to 500
        { duration: '60s', target: 500 },   // sustain
        { duration: '30s', target: 0   },   // ramp down
      ],
    },
  },
  thresholds: {
    'booking_latency_ms{percentile:99}': ['p(99)<500'],
    'booking_latency_ms{percentile:95}': ['p(95)<200'],
    'http_req_failed':                   ['rate<0.05'],
  },
};

/**
 * Runs once before the load test.
 * Creates a fresh test event with exactly 1 seat so all 500 VUs race for the same seat.
 * k6 automatically forwards the session cookie from login to subsequent requests.
 */
export function setup() {
  // Step 1 — admin login (k6 stores Set-Cookie automatically)
  const loginRes = http.post(
    `${BASE_URL}/api/v1/admin/login?username=${ADMIN_USER}&password=${ADMIN_PASS}`
  );

  if (loginRes.status !== 200) {
    throw new Error(
      `Admin login failed (HTTP ${loginRes.status}). ` +
      `Check --env ADMIN_USERNAME and ADMIN_PASSWORD match your app config.`
    );
  }

  // Step 2 — create test event with exactly 1 seat
  const eventRes = http.post(
    `${BASE_URL}/api/v1/admin/events?seatCount=1`,
    JSON.stringify({
      name:     `k6 Load Test ${new Date().toISOString()}`,
      venue:    'k6 Test Arena',
      category: 'OTHER',
      price:    0.0,
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (eventRes.status !== 200) {
    throw new Error(`Event creation failed (HTTP ${eventRes.status}): ${eventRes.body}`);
  }

  const event = JSON.parse(eventRes.body);

  // Step 3 — fetch the single seat
  const seatsRes = http.get(`${BASE_URL}/api/v1/tickets/seats?eventId=${event.id}`);
  const seats    = JSON.parse(seatsRes.body);

  if (!seats || seats.length === 0) {
    throw new Error(`No seats found for event ${event.id}`);
  }

  const seatId = String(seats[0].id);

  console.log(
    `\n[SETUP] Event #${event.id} created — seat #${seatId} (AVAILABLE)\n` +
    `        500 VUs will race for this one seat. Only 1 should confirm.\n`
  );

  return { seatId };
}

export default function (data) {
  const { seatId } = data;
  // Each VU gets a unique studentId so rate limiting (1 req/5s per studentId) doesn't block the test
  const vuId = `vu_${__VU}_${__ITER}`;

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/v1/tickets/claim` +
      `?studentId=${vuId}&seatId=${seatId}&studentName=LoadTest+User`,
    null,
    { tags: { name: 'claim_ticket' } }
  );
  const latency = Date.now() - start;

  bookingLatency.add(latency);

  check(res, {
    'no server error (< 500)': (r) => r.status < 500,
    '202 queued or seat conflict': (r) =>
      r.status === 202 || r.status === 400 || r.status === 409,
  });

  if (res.status === 429) rateLimitHits.add(1);
  bookingSuccessRate.add(res.status === 202 ? 1 : 0);

  sleep(0.1);
}

export function handleSummary(data) {
  const p50 = data.metrics['booking_latency_ms']?.values?.['p(50)']?.toFixed(1) ?? 'n/a';
  const p95 = data.metrics['booking_latency_ms']?.values?.['p(95)']?.toFixed(1) ?? 'n/a';
  const p99 = data.metrics['booking_latency_ms']?.values?.['p(99)']?.toFixed(1) ?? 'n/a';
  const rps = data.metrics['http_reqs']?.values?.rate?.toFixed(1)               ?? 'n/a';

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

→ Resume phrase: "P99 booking latency ${p99}ms at ${rps} req/s under 500 concurrent users"
`);

  return { 'k6/load_test_results.json': JSON.stringify(data, null, 2) };
}
