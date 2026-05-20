import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

const BASE_URL = 'http://localhost:8080';
const TOTAL_USERS = 250;
const PARKING_LOT_ID = 1;  // 확인된 값으로 고정
const PARKING_SPOT_ID = 1; // 확인된 값으로 고정

function getTomorrowDateTime(hourOffset = 0) {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(10 + hourOffset, 0, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export const options = {
  setupTimeout: '300s',
  scenarios: {
    cas_concurrency_test: {
      executor: 'per-vu-iterations',
      vus: TOTAL_USERS,
      iterations: 1,
      maxDuration: '10m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
  },
};

export function setup() {
  const tokens = [];

  for (let i = 0; i < TOTAL_USERS; i++) {
    const email = `castest_${i + 1}@parkeasy.com`;
    const plateNumber = `${String(i + 1).padStart(2, '0')}가${String(i + 1).padStart(4, '0')}`;

    // 회원가입 (이미 있으면 실패해도 무시)
    http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({
        userEmail: email,
        password: 'Test1234!',
        name: `테스트유저${i + 1}`,
        plateNumber: plateNumber,
        vehicleType: 'SMALL',
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    // 로그인은 항상 시도
    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ userEmail: email, password: 'Test1234!' }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    let token = null;
    try {
      const body = JSON.parse(loginRes.body);
      token = body.data?.accessToken ?? null;
    } catch (e) {
      console.error(`[setup] ${email} 로그인 파싱 실패: ${loginRes.body}`);
    }

    if (!token) {
      console.error(`[setup] ${email} 토큰 없음 - 상태코드: ${loginRes.status}`);
    }

    tokens.push(token);

    if ((i + 1) % 50 === 0) {
      const successCount = tokens.filter(t => t !== null).length;
      console.log(`[setup] ${i + 1}/${TOTAL_USERS}명 완료 (토큰 획득: ${successCount}개)`);
    }
  }

  const validTokenCount = tokens.filter(t => t !== null).length;
  console.log(`[setup] 완료 - 유효 토큰: ${validTokenCount}/${TOTAL_USERS}`);
  console.log(`[setup] parkingLotId: ${PARKING_LOT_ID}, parkingSpotId: ${PARKING_SPOT_ID}`);

  return { tokens };
}

export default function (data) {
  const vuId = exec.vu.idInTest;
  const token = data.tokens[vuId - 1];

  if (!token) {
    console.error(`[VU ${vuId}] 토큰 없음 - 스킵`);
    return;
  }

  const startTime = getTomorrowDateTime(0); // 내일 10:00
  const endTime = getTomorrowDateTime(2); // 내일 12:00

  const res = http.post(
    `${BASE_URL}/api/reservations`,
    JSON.stringify({
      parkingLotId: PARKING_LOT_ID,
      parkingSpotId: PARKING_SPOT_ID,
      startTime: startTime,
      endTime: endTime,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    }
  );

  check(res, {
    '예약 성공(200)': (r) => r.status === 200,
    'CAS 충돌(409)': (r) => r.status === 409,
    '예상 외 에러 없음': (r) => r.status === 200 || r.status === 409,
  });

  if (res.status !== 200 && res.status !== 409) {
    console.error(`[VU ${vuId}] 예상 외 응답: ${res.status} - ${res.body}`);
  }
}