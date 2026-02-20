import React, { Component } from 'react';
import { Button, Modal, ModalHeader, ModalBody, ModalFooter, Container } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import Iframe from 'react-iframe';
import MessageAlert from '../MessageAlert';
import { errorMessage } from './Util';
import { loading } from '../common/Loading';
import { getResponse } from "../services/ApiService";
import DOMPurify from "dompurify";
import { marginLeft } from '../common/Util';
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';

class ModalPopup extends Component {
  constructor(props) {
      super(props);
      this.state = {
        modal: false,
        jwt: props.jwt,
        link: props.link,
        isAuthenticated: props.isAuthenticated,
        authorities: props.authorities,
        isLoading: false,
        displayError: null,
        htmlText: null,
        displayAlert: false,
        expanded: false
      };

      this.toggle = this.toggle.bind(this);
  }

  toggle() {
      this.setState({
          modal: !this.state.modal
      });
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      let jwt = this.state.jwt;
      let permissions = this.state.authorities;
      if (jwt && permissions) {
        if (!permissions.some(item => item === 'ROLE_ADMIN')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          this.setState({ modal: true });
        }
      }
    } finally {
      this.setLoading(false);
    }
  };

  async getGrafanaPage() {
    try {
      console.log("getGrafanaPage");
      const { link, jwt } = this.state;
      await getResponse(link, true, true, jwt, true, true)
        .then((response) => {
          console.log(`Content-Type Header: ${response.headers.get("Content-Type")}`);
          if (response.headers.get("Content-Type").indexOf("application/json")>=0) {
            this.setState({ displayError: errorMessage(response.json()) });
            return null;
          }
          return response.text();
        }).then((content) => {
          let contentHTML = DOMPurify.sanitize(content);
          console.log(`contentHTML: ${contentHTML}`);
          this.setState({ 
            htmlText: `${content}`, 
            modal: true
          });
      });
      this.setState({ isLoading: false });
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    }
  }

  render() {
    const { isAuthenticated, authorities, expanded, modal, link, isLoading, displayError, htmlText, displayAlert } = this.state;
    const jsonError = { 'error': 'Only user with ADMIN role can access this page!' };
    const message = errorMessage(JSON.stringify(jsonError));

    const displayContent = () => {
      if (displayAlert) {
        return (<MessageAlert {...message}></MessageAlert>);
      }
      return (
        <div>
          <Modal isOpen={modal} toggle={this.toggle} style={{maxWidth: '90%', maxHeight: '80%'}}>
            <ModalHeader toggle={this.toggle}>Admin Access</ModalHeader>
            <ModalBody style={{height: '70vh'}}>
              <iframe src={link} title="admin-iframe" width="100%" height="100%" frameBorder="0"></iframe>
            </ModalBody>
            <ModalFooter>
              <Button color="secondary" onClick={this.toggle}>Close</Button>
            </ModalFooter>
          </Modal>
        </div>);
    }


    return (
      <div
      style={{
          marginLeft: (isAuthenticated ? marginLeft(expanded) : 0),
          padding: (isAuthenticated ? '15px 20px 0 20px' : '')
      }}
      >
        <AppNavbar />
        <Container fluid>
          <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
          {loading(isLoading)}
          {!isLoading && displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    )

    
  }
}

export default ModalPopup;