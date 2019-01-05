import constants from '../constants/AppConstant';
const { API_V1 } = constants

export async function get(resource, isCredential) {
    try {
        let response;
        if(isCredential) {
            response = await fetch(`${API_V1}/${resource}`,
            { credentials: 'include' });
        } else {
            response = await fetch(`${API_V1}/${resource}`)
        }
        return await response.text();
    } catch (error) {
        throw Error(error)
    }
}

export async function post(resource, payload) {
    try {
        const response = await fetch(`${API_V1}/${resource}`, {
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

