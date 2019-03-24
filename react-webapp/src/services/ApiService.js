import constants from '../constants/AppConstant';

const { API_V1 } = constants;
const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;

export async function get(resource, isCredential, isWithoutApi, jwt) {
    try {
        console.log("Gateway Url: ", gatewayUrl);
        let response;
        let url = (isWithoutApi ? `${gatewayUrl}/${resource}` : `${gatewayUrl}/${API_V1}/${resource}`);
        if(isCredential) {
            response = await fetch(url, {
            headers: {
                'Authorization': jwt
            }});
        } else {
            response = await fetch(url)
        }
        return await response.text();
    } catch (error) {
        throw Error(error)
    }
}

export async function post(resource, payload) {
    try {
        console.log("Gateway Url: ", gatewayUrl);
        const response = await fetch(`${gatewayUrl}/${API_V1}/${resource}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
        return response.json();
    } catch (error) {
        throw Error(error)
    }
}

