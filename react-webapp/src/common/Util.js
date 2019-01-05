export function errorMessage(message) {
    if (message !== null) {
        if(typeof message === 'string') {
            return { type: 'danger', message: { error: message } }
        } else  {
            return { type: 'danger', message: {message}  }
        }
    }
}