import React from 'react';
import ReactJson from 'react18-json-view';
import { toast } from 'react-toastify';
import 'react18-json-view/src/style.css';

function MessageAlert({ typeError, message }) {
    if (typeError === undefined) {
        typeError = "error";
    }
    const options = {
        type: typeError,
        toastId: 'Error'
    }
    const notify = (message, options) => toast(message, options);
    return message ? notify(<div>Response Error:
        <ReactJson
            enableClipboard={true}
            dark={true}
            theme='github'
            src={message} />
        </div>, options) : '';
}

export default MessageAlert

