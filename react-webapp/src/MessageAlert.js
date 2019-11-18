import React from 'react';
import ReactJson from 'react-json-view';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

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
            enableClipboard={false}
            displayObjectSize={false}
            name={false}
            displayDataTypes={false}
            theme="bright"
            src={message} />
        </div>, options) : '';
}

export default MessageAlert

