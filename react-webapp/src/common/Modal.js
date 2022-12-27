import React from 'react';
import { Modal } from 'reactstrap';
import Iframe from 'react-iframe';
import MessageAlert from '../MessageAlert';
import { errorMessage } from './Util';
import './Modal.css';

class ModalPopup extends React.Component {
  constructor(props) {
      super(props);
      this.state = {
          modal: props.modal,
          link: props.link,
          isAuthenticated: props.isAuthenticated,
          authorities: props.authorities      
      };

      this.toggle = this.toggle.bind(this);
  }

  toggle() {
      this.setState({
          modal: !this.state.modal
      });
  }

  render() {
    const { isAuthenticated, authorities, modal, link } = this.state;
    const jsonError = { 'error': 'Only user with ADMIN role can access this page!' };
    const message = errorMessage(JSON.stringify(jsonError));

    return (!isAuthenticated || !authorities.some(item => item === "ROLE_ADMIN") ?
        <MessageAlert {...message}></MessageAlert>
      :
      <div>
        <Modal isOpen={modal} size="lg" toggle={this.toggle} contentClassName="custom-modal">
          {/*
          <ModalHeader toggle={this.toggle}>Admin Access</ModalHeader>
          <ModalBody>
          */}
              <Iframe url={link}
                      position="absolute"
                      width="100%"
                      height="100%" />
          
          {/*
          </ModalBody>
          <ModalFooter>
            <Button color="secondary" onClick={this.toggle}>Close</Button>
          </ModalFooter>
          */}
        </Modal>
      </div>
    );
  }
}

export default ModalPopup;