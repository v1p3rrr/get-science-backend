import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

/**
 * install k6, run using: <br>
 * k6 run load-test.js
 * <br> see output in summary.html
 */

export const options = {
  scenarios: {
    search_events: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
      stages: [
        { target: 80, duration: '3m' }, // разгон до 80 rps
        { target: 80, duration: '3m' }, // держим
        { target: 0, duration: '1m' },   // спад
      ],
      exec: 'searchEvents',
    },
    get_metadata: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 300,
      stages: [
        { target: 20, duration: '3m' },
        { target: 20, duration: '3m' },
        { target: 0, duration: '1m' },
      ],
      exec: 'getMetadata',
    },
    get_event_by_id: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 30,
      maxVUs: 150,
      stages: [
        { target: 10, duration: '3m' },
        { target: 10, duration: '3m' },
        { target: 0, duration: '1m' },
      ],
      exec: 'getEventById',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% запросов < 500мс
    http_req_failed: ['rate<0.01'],   // < 1% ошибок
    checks: ['rate>0.99']
  },
};

const BASE_URL = 'https://get-science-events.ru';

export function searchEvents() {
  const payload = JSON.stringify({
    page: 0,
    size: 12,
    title: '',
    formats: [],
    themes: [],
    locations: [],
    types: [],
  });

  const res = http.post(`${BASE_URL}/api/v1/events/search`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Origin': BASE_URL,
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}

export function getMetadata() {
  const res = http.get(`${BASE_URL}/api/v1/events/filters`);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}

export function getEventById() {
  const randomId = Math.floor(Math.random() * 20) + 1; // [1..20]
  const res = http.get(`${BASE_URL}/api/v1/events/${randomId}`);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}

export function handleSummary(data) {
  return {
    'summary.html': htmlReport(data),
  };
}
