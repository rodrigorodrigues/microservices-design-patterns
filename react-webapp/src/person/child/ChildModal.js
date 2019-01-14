import React from 'react';
import {Button, Modal, ModalHeader, ModalBody, ModalFooter, Table} from 'reactstrap';

class ChildModal extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            modal: false
        };

        this.toggle = this.toggle.bind(this);
    }

    toggle() {
        this.setState({
            modal: !this.state.modal
        });
    }

    render() {
        const childrenList = this.props.user.children ? this.props.user.children.map((child, index) => {
            return <tr key={index}>
                <td style={{whiteSpace: 'nowrap'}}>{child.name}</td>
                <td>{child.age}</td>
            </tr>
        }) : '';

        return (
            <div>
                <Button color="danger" onClick={this.toggle}>Show</Button>
                <Modal isOpen={this.state.modal} toggle={this.toggle} className={this.props.className}>
                    <ModalHeader toggle={this.toggle}>Children</ModalHeader>
                    <ModalBody>
                        <Table className="mt-4">
                            <thead>
                            <tr>
                                <th width="10%">Name</th>
                                <th width="5%">Age</th>
                            </tr>
                            </thead>
                            <tbody>
                            {childrenList}
                            </tbody>
                        </Table>
                    </ModalBody>
                    <ModalFooter>
                        <Button color="secondary" onClick={this.toggle}>Cancel</Button>
                    </ModalFooter>
                </Modal>
            </div>
        );
    }
}

export default ChildModal;