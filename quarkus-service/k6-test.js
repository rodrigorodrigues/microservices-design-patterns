import http from 'k6/http';
import { sleep, check } from 'k6';
import faker from "https://cdnjs.cloudflare.com/ajax/libs/Faker/3.1.0/faker.min.js";

export default function () {
    // console.log("name: " + faker.name.findName());

    // send custom payload/post data
    const payload = JSON.stringify({
        name: faker.company.companyName(),
        activated: true
    });

    // send post request with custom header and payload
    const url = 'http://localhost:8081/api/quarkus/persons';
    let res = http.post(url, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer eyJraWQiOiJ0ZXN0IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJUZXN0IiwibmJmIjoxNjY1NDM5MjUzLCJzY29wZSI6InJlYWQiLCJpc3MiOiJqd3QiLCJleHAiOjE2NjU1MjU2NTMsImlhdCI6MTY2NTQzOTI1MywiYXV0aG9yaXRpZXMiOlsiQURNSU4iLCJST0xFX0FETUlOIl0sImp0aSI6ImJjNjJkMGIxLTc1ZTgtNGJmMy04N2JkLWI2M2M4MGYwOTBkNCJ9.bHwMVudNEM1akf13hOvlFgM_q255aWQHTyU5rgbACtbtW6OFRNT0x5YNoEjf3MsIo_3q4--ZDTuDHz8Bp_SUmShRn_JjTirVTr2dSfTcmjhAtbcvr6zDY5K_v6p33K_Qf1KX-owrUZ4qXWlVgqVpushXzwwAX70qATYCYorRI1iY7o97XYI7W_U10S0nhGKQ9ENPjHGM-woxLEbLowdQBatWaIt12n3Jh0Zq0bK1pXPeBIb6oyFuxhIovOv9AgCoLWU497kINTnl-Rsi1d7gGc2HSSqiH2i2m256LpTxBYbhR4QDEBlJsolEsFVVBiTh5Q4MKSxsxZMvgJmV7dbRrA'
        }
    });

    check(res, {
        'is status 200': (res) => res.status === 201,
        'body size is > 0': (r) => r.body.length > 0,
    });

    console.log("resp body: " + res.body);

    res = http.get(`${url}/${res.body.id}`);
    check(res, {
        'is status 200': (res) => res.status === 200,
        'body size is > 0': (r) => r.body.length > 0,
    });
    sleep(1);
}