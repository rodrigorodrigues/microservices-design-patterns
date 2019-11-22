import React from 'react';
import { Table } from 'reactstrap';

const FooterContent = () => {
    return (
        <footer>
            <div className="float-left">
                <Table size="sm" borderless>
                    <tr>
                        <td>Microservice Design Patterns</td>
                        <td><a href="https://github.com/rodrigorodrigues/microservices-design-patterns">GitHub</a></td>
                        <td>Version: <b>{process.env.REACT_APP_VERSION}</b></td>
                    </tr>
                </Table>
            </div>
            <div className="float-right"><a href="https://spendingbetter.com">https://spendingbetter.com</a></div>
        </footer>
    )
}

export default FooterContent