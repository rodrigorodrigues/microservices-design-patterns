import constants from '../constants/AppConstant';

const { API_V1 } = constants;
const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;

export async function getWithCredentials(resource, isWithoutApi) {
    return get(resource, false, isWithoutApi);
}

export async function get(resource, isCredential, isWithoutApi, jwt) {
    try {
        let response;
        let url = (isWithoutApi ? `${gatewayUrl}/${resource}` : `${gatewayUrl}/${API_V1}/${resource}`);
        console.log("API Url: ", url);
        if(isCredential) {
            response = await fetch(url, {
                credentials: 'include',
                headers: {
                    'Authorization': jwt
                }
            });
        } else {
            response = await fetch(url, {credentials: 'include'})
        }
        console.log("response status: ", response.status);
        console.log("response header content-type: ", response.headers.get('Content-type'));
        if (response.headers.get('Content-type') !== null && response.headers.get('Content-type').startsWith('application/json')) {
            return response.json();
        } else {
            return response.text();
        }
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
            credentials: 'include',
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