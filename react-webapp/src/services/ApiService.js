import constants from '../constants/AppConstant';

const { API_V1 } = constants;
const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;

export async function getWithoutCredentials(resource, isWithoutApi) {
    return get(resource, false, isWithoutApi);
}

export async function get(resource, isCredential, isWithoutApi, jwt) {
    try {
        let response;
        let url = (isWithoutApi ? `${gatewayUrl}/${resource}` : `${gatewayUrl}/${API_V1}/${resource}`);
        console.log("API Url: ", url);
        if(isCredential) {
            response = await fetch(url, {
            headers: {
                'Authorization': jwt
            }});
        } else {
            response = await fetch(url)
        }
        return await response.text();
    } catch (e) {
        console.log("Error get method", e);
        throw Error(e)
    }
}

export async function postWithHeaders(resource, payload, headers) {
    return processPost(resource, headers, payload);
}

export async function post(resource, payload) {
    return processPost(resource, {'Content-Type': 'application/json'}, JSON.stringify(payload));
}

async function processPost(resource, headers, body) {
    try {
        const url = `${gatewayUrl}/${API_V1}/${resource}`;
        console.log("API Url: ", url);
        const response = await fetch(url, {
            method: 'POST',
            headers: headers,
            body: body
        });
        console.log("response status: ", response.status);
        return response.json();
    } catch (e) {
        console.log("Error post method", e);
        throw Error(e)
    }
}