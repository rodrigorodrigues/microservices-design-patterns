import http from 'k6/http';
import { sleep, check } from 'k6';
import faker from "https://cdnjs.cloudflare.com/ajax/libs/Faker/3.1.0/faker.min.js";

export default function () {
    // console.log("name: " + faker.name.findName());

    // send custom payload/post data
    const payload = JSON.stringify({
        name: `${faker.company.companyName()}`,
        fullName: `${faker.name.firstName()} ${faker.name.lastName()}`,
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
    const url = 'https://spendingbetter.com/api/companies';
    let res = http.post(url, payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJyb2RyaWdvcm9kcmlndWVzd2ViQGdtYWlsLmNvbSIsImF1dGgiOiJST0xFX1VTRVIsU0NPUEVfaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vYXV0aC91c2VyaW5mby5lbWFpbCxTQ09QRV9odHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9hdXRoL3VzZXJpbmZvLnByb2ZpbGUsU0NPUEVfb3BlbmlkIiwidXNlcl9uYW1lIjoiMTE0MTMwOTE2NDc2MzA4MDM4OTk5IiwiaXNzIjoiaHR0cHM6Ly9zcGVuZGluZ2JldHRlci5jb20iLCJmdWxsTmFtZSI6IlJvZHJpZ28gUm9kcmlndWVzIiwidHlwZSI6ImFjY2VzcyIsImF1dGhvcml0aWVzIjpbIlNDT1BFX2h0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL2F1dGgvdXNlcmluZm8uZW1haWwiLCJTQ09QRV9vcGVuaWQiLCJST0xFX1VTRVIiLCJTQ09QRV9odHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9hdXRoL3VzZXJpbmZvLnByb2ZpbGUiXSwiY2xpZW50X2lkIjoiMTE0MTMwOTE2NDc2MzA4MDM4OTk5IiwiYXVkIjoiaHR0cHM6Ly9zcGVuZGluZ2JldHRlci5jb20iLCJuYmYiOjE2NzA3NjEyNTQsInNjb3BlIjpbInJlYWQiXSwiaW1hZ2VVcmwiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BRWRGVHA1UG5sY3ZJR0dHUWFqZEM2dS1wQUlsNGxWZFBXWV84aG5lOWR5S2JnPXM5Ni1jIiwibmFtZSI6InJvZHJpZ29yb2RyaWd1ZXN3ZWJAZ21haWwuY29tIiwiZXhwIjoxNjcwNzYzMDU0LCJmcmVzaCI6dHJ1ZSwiaWF0IjoxNjcwNzYxMjU0LCJqdGkiOiJmNWQxNDMzYS1kZGRkLTQxOGEtYmE3OS00YzlhOTllODQxODkifQ.2JGtDncqK0MZW-fDTodUGoJd9xOdtDXsCmrSyScKS4paMYYyw6nhZS4aUIQGskPixX3HjEutHI4SHP6Nk7jKzrWj_GZOZ-hOBxE7O_0gQSGZGR9jyy4s-1lgFKhGt1miOdKi_xoPg27UR4zv7ih0IV8yx73CNCI-NHjEqyi4Y-Cc1LN6x3NARODjjkR5KrIfg2j4_sugunfo-TlmzshWQwIaqhd5eLnng0p6Y3ZIVZSbF6ATYjaSh9I6WsHdshXrRZJelWVT933umwHgH4BEwtEFuDjVKmJd2NkL3MjGEHhMgP95sOmV_20oHYv74d2kGJeigcmThc7rZpJKxslT8w'
        }
    });

    check(res, {
        'is status 200': (res) => res.status === 201,
        'body size is > 0': (r) => r.body.length > 0,
    });

    console.log("resp body: " + res.body);

    res = http.get(`${res.headers['Location']}`, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJyb2RyaWdvcm9kcmlndWVzd2ViQGdtYWlsLmNvbSIsImF1dGgiOiJST0xFX1VTRVIsU0NPUEVfaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vYXV0aC91c2VyaW5mby5lbWFpbCxTQ09QRV9odHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9hdXRoL3VzZXJpbmZvLnByb2ZpbGUsU0NPUEVfb3BlbmlkIiwidXNlcl9uYW1lIjoiMTE0MTMwOTE2NDc2MzA4MDM4OTk5IiwiaXNzIjoiaHR0cHM6Ly9zcGVuZGluZ2JldHRlci5jb20iLCJmdWxsTmFtZSI6IlJvZHJpZ28gUm9kcmlndWVzIiwidHlwZSI6ImFjY2VzcyIsImF1dGhvcml0aWVzIjpbIlNDT1BFX2h0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL2F1dGgvdXNlcmluZm8uZW1haWwiLCJTQ09QRV9vcGVuaWQiLCJST0xFX1VTRVIiLCJTQ09QRV9odHRwczovL3d3dy5nb29nbGVhcGlzLmNvbS9hdXRoL3VzZXJpbmZvLnByb2ZpbGUiXSwiY2xpZW50X2lkIjoiMTE0MTMwOTE2NDc2MzA4MDM4OTk5IiwiYXVkIjoiaHR0cHM6Ly9zcGVuZGluZ2JldHRlci5jb20iLCJuYmYiOjE2NzA3NjEyNTQsInNjb3BlIjpbInJlYWQiXSwiaW1hZ2VVcmwiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BRWRGVHA1UG5sY3ZJR0dHUWFqZEM2dS1wQUlsNGxWZFBXWV84aG5lOWR5S2JnPXM5Ni1jIiwibmFtZSI6InJvZHJpZ29yb2RyaWd1ZXN3ZWJAZ21haWwuY29tIiwiZXhwIjoxNjcwNzYzMDU0LCJmcmVzaCI6dHJ1ZSwiaWF0IjoxNjcwNzYxMjU0LCJqdGkiOiJmNWQxNDMzYS1kZGRkLTQxOGEtYmE3OS00YzlhOTllODQxODkifQ.2JGtDncqK0MZW-fDTodUGoJd9xOdtDXsCmrSyScKS4paMYYyw6nhZS4aUIQGskPixX3HjEutHI4SHP6Nk7jKzrWj_GZOZ-hOBxE7O_0gQSGZGR9jyy4s-1lgFKhGt1miOdKi_xoPg27UR4zv7ih0IV8yx73CNCI-NHjEqyi4Y-Cc1LN6x3NARODjjkR5KrIfg2j4_sugunfo-TlmzshWQwIaqhd5eLnng0p6Y3ZIVZSbF6ATYjaSh9I6WsHdshXrRZJelWVT933umwHgH4BEwtEFuDjVKmJd2NkL3MjGEHhMgP95sOmV_20oHYv74d2kGJeigcmThc7rZpJKxslT8w'
        }
    });

    check(res, {
        'is status 200': (res) => res.status === 200,
        'body size is > 0': (r) => r.body.length > 0,
    });
    sleep(1);
}