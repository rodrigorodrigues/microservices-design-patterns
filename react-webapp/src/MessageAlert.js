import React from 'react';
import { Alert } from 'reactstrap';

function MessageAlert({ type, message }) {
    return <Alert color={type}>
        {message}
    </Alert>
}

export default MessageAlert

