export function errorMessage(message) {
    if (message !== null) {
        message = JSON.stringify(message);
        if(typeof message === 'string') {
            return { type: 'danger', message: {message} }
        } else  {
            return { type: 'danger', message: {message}  }
        }
    }
}