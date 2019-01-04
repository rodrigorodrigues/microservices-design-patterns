export function errorMessage(message) {
    if (message !== null) {
        if(typeof message === 'string') {
            return { type: 'danger', message: { error: message } }
        } else  {
            const text = message['message'] ? 
                message['message'] :  
                message['error'] 
            return { type: 'danger', message: {error: text}  }
        }
    }
}