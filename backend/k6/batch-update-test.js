import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
    vus: 100, // 100명의 동시 사용자
    duration: '30s',
};

const BASE_URL = 'http://192.168.45.205:8080/stock/decrease-bulk-update'; // 다중 처리 엔드포인트
const TEST_TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDEiLCJ1c2VybmFtZSI6Iuq0gOumrOyekCIsInVzZXJFbWFpbCI6ImFkbWluQG5hdmVyLmNvbSIsInVzZXJSb2xlIjoiVVNFUiIsImlhdCI6MTc3MzkxMDQ2NSwiZXhwIjoxNzczOTE0MDY1fQ.r3mA5CoIWpKssh-F196z8XHi5sznQHzhW1R0g0rj_MQ';

// 1~100 사이의 랜덤한 숫자(상품 ID)를 뽑는 함수
function getRandomProductId() {
    return Math.floor(Math.random() * 100) + 1;
}

export default function () {
    // 가상 유저(VU) 번호가 짝수인지 홀수인지 판별하여 그룹을 나눔
    const isEven = __VU % 2 === 0;
    const payloadArray = [];

    if (isEven) {
        // 그룹 A (짝수 유저): 1번부터 20번까지 오름차순 정렬 요청
        for (let i = 1; i <= 20; i++) {
            payloadArray.push({ productId: i, quantity: 1 });
        }
    } else {
        // 그룹 B (홀수 유저): 20번부터 1번까지 역순(내림차순) 정렬 요청
        for (let i = 20; i >= 1; i--) {
            payloadArray.push({ productId: i, quantity: 1 });
        }
    }

    const payload = JSON.stringify(payloadArray);

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': TEST_TOKEN,
        },
    };

    http.post(BASE_URL, payload, params);

    // 락 충돌을 극대화하기 위해 대기 시간을 거의 없앰
    sleep(1);
}