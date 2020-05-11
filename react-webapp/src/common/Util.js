export function errorMessage(message) {
    if (message !== null) {
        if(typeof message === 'string') {
            try {
                message = JSON.parse(message);
            } catch (e) {
                console.log("Invalid json content: ", message);
            }
            return { type: 'danger', message: message }
        } else  {
            return { type: 'danger', message: message  }
        }
    }
}