import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

const BASE_URL = 'http://localhost:8080';
const TOTAL_USERS = 250;

// 날짜 헬퍼: 내일 날짜로 예약 (오늘+1일, validateTime 통과)
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
    // 참고용 — 통과/실패 기준 아님
    http_req_duration: ['p(95)<3000'],
  },
};

export function setup() {
  const tokens = [];
  let parkingLotId = null;
  let parkingSpotId = null;

  // 1. 유저 250명 회원가입 + 로그인
  for (let i = 0; i < TOTAL_USERS; i++) {
    const email = `castest_${i + 1}@parkeasy.com`;
    const plateNumber = `${String(i + 1).padStart(2, '0')}가${String(i + 1).padStart(4, '0')}`;

    // 회원가입
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

    // 로그인
    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ userEmail: email, password: 'Test1234!' }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    const body = JSON.parse(loginRes.body);
    tokens.push(body.data?.accessToken ?? null);

    if ((i + 1) % 50 === 0) {
      console.log(`[setup] 유저 ${i + 1}/${TOTAL_USERS}명 완료`);
    }
  }

  // 2. 주차장/주차 자리 ID 확인 (관리자 또는 공개 API)
  // 실제 환경에 맞게 parkingLotId, parkingSpotId 를 직접 지정하거나
  // 아래처럼 API로 조회
  const lotRes = http.get(`${BASE_URL}/api/parking-lots`, {
    headers: { 'Content-Type': 'application/json' },
  });

  try {
    const lotBody = JSON.parse(lotRes.body);
    // 첫 번째 주차장의 첫 번째 SMALL 자리 사용
    // 실제 응답 구조에 맞게 수정 필요
    parkingLotId = lotBody.data?.[0]?.id ?? null;
  } catch (e) {
    console.error('[setup] 주차장 조회 실패 - parkingLotId를 직접 지정하세요');
  }

  // parkingSpotId는 직접 지정 권장 (250명이 같은 자리를 노려야 하므로)
  // DB에서 확인 후 아래 값을 실제 ID로 교체
  parkingSpotId = 1; // ← 실제 SMALL 자리 ID로 교체 필요

  console.log(`[setup] parkingLotId: ${parkingLotId}, parkingSpotId: ${parkingSpotId}`);
  console.log('[setup] 완료. 동시 예약 테스트 시작.');

  return { tokens, parkingLotId, parkingSpotId };
}

// 250명이 동시에 같은 자리에 예약 시도
export default function (data) {
  const vuId = exec.vu.idInTest;
  const token = data.tokens[vuId - 1];
  const { parkingLotId, parkingSpotId } = data;

  if (!token || !parkingLotId || !parkingSpotId) return;

  const startTime = getTomorrowDateTime(0); // 내일 10:00
  const endTime = getTomorrowDateTime(2); // 내일 12:00

  const res = http.post(
    `${BASE_URL}/api/reservations`,
    JSON.stringify({
      parkingLotId: parkingLotId,
      parkingSpotId: parkingSpotId,
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

  // 200: 예약 성공 (1명만 가능)
  // 409: CAS 충돌로 선점 실패 → 정상적인 동시성 처리
  // 나머지: 예상 외 에러
  check(res, {
    '예약 성공(200)': (r) => r.status === 200,
    'CAS 충돌(409)': (r) => r.status === 409,
    '예상 외 에러 없음': (r) => r.status === 200 || r.status === 409,
  });

  if (res.status !== 200 && res.status !== 409) {
    console.error(`[VU ${vuId}] 예상 외 응답: ${res.status} - ${res.body}`);
  }
}