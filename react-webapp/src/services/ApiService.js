import constants from '../constants/AppConstant';

const { API_V1 } = constants;
const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;

export async function getWithCredentials(resource, isWithoutApi) {
    return get(resource, false, isWithoutApi);
}

export async function get(resource, isCredential, isWithoutApi, jwt) {
    return getResponse(resource, isCredential, isWithoutApi, jwt, false, false);
}

export async function getResponse(resource, isCredential, isWithoutApi, jwt, returnResponseObject, isUrl) {
    try {
        let response;
        let url = (isUrl ? resource : (isWithoutApi ? `${gatewayUrl}/${resource}` : `${gatewayUrl}/${API_V1}/${resource}`));
        console.log("API Url: ", url);
        if(isCredential) {
            response = await fetch(url, {
                credentials: 'include',
                headers: {
                    'Authorization': jwt
                }
            });
        } else {
            response = await fetch(url, {
                credentials: 'include'
            });
        }
        if (!returnResponseObject) {
            console.log("response status: ", response.status);
            const contentTypeHeader = response.headers.get('Content-type');
            if (contentTypeHeader !== null && contentTypeHeader.startsWith('application/json')) {
                return response.json();
            } else {
                return response.text();
            }
        } else {
            return response;
        }
    } catch (e) {
        console.log("Error get method", e);
        throw Error(e)
    }
}

export async function postWithHeaders(resource, payload, headers) {
    return processPost(resource, headers, payload, false);
}

export async function postWithHeadersAndApi(resource, payload, headers, isWithoutApi) {
    return processPost(resource, headers, payload, isWithoutApi);
}

export async function post(resource, payload) {
    return processPost(resource, {'Content-Type': 'application/json'}, JSON.stringify(payload), true);
}

async function processPost(resource, headers, body, isWithoutApi) {
    try {
        const url = (isWithoutApi ? `${gatewayUrl}/${resource}` : `${gatewayUrl}/${API_V1}/${resource}`);
        console.log("API Url: ", url);
        const response = await fetch(url, {
            credentials: 'include',
            method: 'POST',
            headers: headers,
            body: body
        });
        console.log("response status: ", response.status);
        const contentTypeHeader = response.headers.get('Content-type');
        if (contentTypeHeader !== null && contentTypeHeader.startsWith('application/json')) {
            return response.json();
        } else {
            return response.text();
        }
    } catch (e) {
        console.log("Error post method", e);
        throw Error(e)
    }
}