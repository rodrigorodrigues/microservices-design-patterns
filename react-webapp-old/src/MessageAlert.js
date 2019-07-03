import React from 'react';
import { Alert } from 'reactstrap';
import ReactJson from 'react-json-view';

function MessageAlert({ type, message }) {
    return message ? <Alert color={type ? type : 'success'}>
        <ReactJson
            enableClipboard={false}
            displayObjectSize={false}
            name={null}
            displayDataTypes={false}
            src={message} />
    </Alert> : ''
}

export default MessageAlert

