import constants from '../constants/AppConstant';
const { API_V1 } = constants

export async function get(resource, isCredential, isWithoutApi) {
    try {
        let response;
        let url = (isWithoutApi ? `${resource}` : `${API_V1}/${resource}`);
        if(isCredential) {
            response = await fetch(url,
            { credentials: 'include' });
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
        const response = await fetch(`http://109.255.172.3:8080/${API_V1}/${resource}`, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
        return response.json();
    } catch (error) {
        throw Error(error)
    }
}

