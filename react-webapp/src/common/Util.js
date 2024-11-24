export function errorMessage(message, type) {
    if (message !== null) {
        if(typeof message === 'string') {
            try {
                message = JSON.parse(message);
            } catch (e) {
                console.log("Invalid json content: ", message);
            }
            return { type: (type !== undefined ? type : 'danger'), message: message }
        } else  {
            return { type: (type !== undefined ? type : 'danger'), message: message  }
        }
    }
}

export function marginLeft(expanded) {
    if (expanded) {
        return 255;
      } else {
        return 40;
    }  
}