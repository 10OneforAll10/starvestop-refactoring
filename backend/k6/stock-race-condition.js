import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
    vus: 100,
    duration: '30s',
};

const BASE_URL = 'http://192.168.45.157:8080/stock/decrease';
const TEST_TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDEiLCJ1c2VybmFtZSI6Iuq0gOumrOyekCIsInVzZXJFbWFpbCI6ImFkbWluQG5hdmVyLmNvbSIsInVzZXJSb2xlIjoiVVNFUiIsImlhdCI6MTc3MzM5NDAzMywiZXhwIjoxNzczMzk3NjMzfQ.h8Ak6hbDcs8Nk1PlU4NwVg2SQQltbtyMNE-cKE5uYNk';

export default function () {
    const payload = JSON.stringify({
        productId: 1,
        quantity: 2
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': TEST_TOKEN,
        },
    };

    const res = http.post(BASE_URL, payload, params);


    sleep(1);
}