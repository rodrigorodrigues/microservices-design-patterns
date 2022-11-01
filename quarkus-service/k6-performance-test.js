import http from 'k6/http';
import { sleep, check } from 'k6';
import faker from "https://cdnjs.cloudflare.com/ajax/libs/Faker/3.1.0/faker.min.js";

export default function () {
    // console.log("name: " + faker.name.findName());

    // send custom payload/post data
    const payload = JSON.stringify({
        fullName: `${faker.name.findName()} ${faker.name.lastName()}`,
        dateOfBirth: '2000-01-10', //faker.date.birthdate(),
        address: {
            address: faker.address.streetAddress(),
            city: faker.address.city(),
            stateOrProvince: faker.address.state(),
            country: faker.address.country(),
            postalCode: faker.address.zipCode()
        }
    });

    console.log(`Payload: ${payload}`);

    // send post request with custom header and payload
    const url = 'https://spendingbetter.com/api/people';
    let res = http.post(url, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJyb2RyaWdvcm9kcmlndWVzd2ViQGdtYWlsLmNvbSIsImF1dGgiOiJST0xFX1VTRVIsU0NPUEVfaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vYXV0aC91c2VyaW5mby5lbWFpbCxTQ09QRV9odHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9hdXRoL3VzZXJpbmZvLnByb2ZpbGUsU0NPUEVfb3BlbmlkIiwidXNlcl9uYW1lIjoiMTE0MTMwOTE2NDc2MzA4MDM4OTk5IiwiaXNzIjoiaHR0cHM6Ly9zcGVuZGluZ2JldHRlci5jb20iLCJmdWxsTmFtZSI6IlJvZHJpZ28gUm9kcmlndWVzIiwidHlwZSI6ImFjY2VzcyIsImF1dGhvcml0aWVzIjpbIlNDT1BFX2h0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL2F1dGgvdXNlcmluZm8uZW1haWwiLCJTQ09QRV9vcGVuaWQiLCJST0xFX1VTRVIiLCJTQ09QRV9odHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9hdXRoL3VzZXJpbmZvLnByb2ZpbGUiXSwiY2xpZW50X2lkIjoiMTE0MTMwOTE2NDc2MzA4MDM4OTk5IiwiYXVkIjoiaHR0cHM6Ly9zcGVuZGluZ2JldHRlci5jb20iLCJuYmYiOjE2NjYwNDgzMDAsInNjb3BlIjpbInJlYWQiXSwiaW1hZ2VVcmwiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BTG01d3UzM1FzeGNBeGNUOVhONjFwZjUxaG5EajVhZUxRSTBYWmtWdXo2eGZBPXM5Ni1jIiwibmFtZSI6InJvZHJpZ29yb2RyaWd1ZXN3ZWJAZ21haWwuY29tIiwiZXhwIjoxNjY2MDUwMTAwLCJmcmVzaCI6dHJ1ZSwiaWF0IjoxNjY2MDQ4MzAwLCJqdGkiOiJiYWVhNzAxNC03MmUzLTQ1MTQtYjEyMS04ZDc1ODViYjBlMTYifQ.w3tu9Kx-tYCZomAhToJsui2-Ov7811EOaF8T_r12Z0GnyaJNxqYd1M13Wr7V5tfXFdXEirTszOy4BbhLcpTy2_1d5UbMGUbOsmXtFquX6XfZw1RR7bmDklMwelq0Hs9X8GPA375T8_SntowtCKiQggCE-k1h_IEpd-26NRyvfaPTU0rj_EdK-RU3xmUb3vHTkv2-ZEItWMt-9o1NLbiCueURK-6CLVzLDebZwwGlVk64AHdTPC1JQJun5LlMCustIp8OH0iVaKcB1AelDd-2hRAAnb70tVX3H15M7r-2PMcKTrc-0ujHYI8ZfKhHpyxi5P0qvzZfQO7f2Jv5egP_1w'
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