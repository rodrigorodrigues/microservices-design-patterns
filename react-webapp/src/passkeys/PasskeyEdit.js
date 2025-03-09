import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {Feedback, Form, FormGroup, Input, Label} from '@availity/form';
import * as yup from 'yup';
import {Button, Container, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import FooterContent from '../home/FooterContent';
import HomeContent from '../home/HomeContent';
import { marginLeft } from '../common/Util';
import { loading } from '../common/Loading';
import uuid from 'react-uuid';
import { getWithCredentials } from '../services/ApiService';

const {client} = require('@passwordless-id/webauthn');

class PasskeyEdit extends Component {
  emptyPasskey = {
    label: '',
    requestId: uuid()
  };

  constructor(props) {
    super(props);
    this.state = {
      passkey: this.emptyPasskey,
      jwt: props.jwt,
      displayError: null,
      displayAlert: false,
      authorities: props.authorities,
      isLoading: false,
      expanded: false,
      isAuthenticated: props.isAuthenticated,
      gatewayUrl: props.gatewayUrl
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  /**
   * Method to convert the base64 to string
   */
   decodeBase64(base64url) {
    const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    const binStr = window.atob(base64);
    const bin = new Uint8Array(binStr.length);
    for (let i = 0; i < binStr.length; i++) {
      bin[i] = binStr.charCodeAt(i);
    }
    return bin.buffer;
  }

  async handleSubmit(event) {
    try {
      const { passkey, jwt, gatewayUrl } = this.state;
      console.log("Passkey", passkey);
      this.setLoading(true);

      const csrfData = await getWithCredentials('csrf', false);
      const csrfToken = csrfData.token;

      // 1. Get a challenge from the server
      const response = await fetch(`${gatewayUrl}/webauthn/register/options`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': jwt,
          'X-XSRF-TOKEN': csrfToken,
          'requestId': uuid()
        },
        credentials: 'include'
      });

      if (response.status !== 200) {
        throw new Error('Invalid passkey register - challenge');
      }

      let data = await response.json();
      
      console.log(`response: ${JSON.stringify(data)}`);

      const decodedExcludeCredentials = !data.excludeCredentials
          ? []
          : data.excludeCredentials.map(cred => ({
              ...cred,
              id: this.decodeBase64(cred.id),
            }));
      console.log(`decodedExcludeCredentials: ${decodedExcludeCredentials}`);

      const decodedOptions = {
        user: {
          id: data.user.id,
          name: data.user.name,
          displayName: data.user.displayName,
        },
        challenge: data.challenge,
        excludeCredentials: decodedExcludeCredentials,
      };

      console.log(`decodedOptions: ${JSON.stringify(decodedOptions)}`);

      // 2. Invoking WebAuthn in the browser
      const registration = await client.register(decodedOptions);
      console.log(`registration: ${JSON.stringify(registration)}`);

      const registrationParsed = {
        publicKey: {
          credential: {
            id: registration.id,
            rawId: registration.rawId,
            response: {
              ...registration.response,
            },
            type: registration.type,
            clientExtensionResults: registration.clientExtensionResults,
            authenticatorAttachment: registration.authenticatorAttachment,
          },
          label: passkey.label,
        },
      };

      console.log(`registrationParsed: ${JSON.stringify(registrationParsed)}`);

      // 3. Send the payload to the server
      const registerResponse = await fetch(`${gatewayUrl}/webauthn/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': jwt,
          'X-XSRF-TOKEN': csrfToken,
          'requestId': uuid()
        },
        body: JSON.stringify(registrationParsed),
        credentials: 'include'
      });

      // 4. Check if response is 200
      if (registerResponse.status !== 200) {
        throw new Error('Invalid passkey register - registration');
      }

      this.props.history.push('/passkeys');
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    } finally {
      this.setLoading(false);
    }
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      const { jwt, isAuthenticated, gatewayUrl } = this.state;
      if (jwt) {
        if (!isAuthenticated) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          if (this.props.match.params.id !== 'new') {
            try {
              const passkey = await (await fetch(`${gatewayUrl}/api/passkeys/${this.props.match.params.id}`, { method: 'GET',      headers: {
                'Content-Type': 'application/json',
                'Authorization': jwt,
                'requestId': uuid()
              }})).json();
              this.setState({passkey: passkey, isLoading: false});
            } catch (error) {
              this.setState({displayAlert: true, sLoading: false, displayError: errorMessage(error)});
            }
          } else {
            this.setState({isLoading: false});
          }
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let passkey = {...this.state.passkey};
    passkey[name] = value;
    this.setState({passkey: passkey});
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  render() {
    const { passkey, displayError, displayAlert, isLoading, expanded } = this.state;
    const title = <h2>{passkey.id ? 'Edit Passkey' : 'Add Passkey'}</h2>;

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
          {title}
          <Form onSubmit={this.handleSubmit}
            enableReinitialize={true}
            initialValues={{
              label: passkey?.label || ''
            }}
            validationSchema={yup.object().shape({
              label: yup.string().trim().required('This field is required.')
            })}
          >
            <FormGroup>
              <Label for="label">Label</Label>
              <Input type="text" name="label" id="label" onChange={this.handleChange} placeholder="Label" />
              <Feedback name="label" />
            </FormGroup>
            <FormGroup>
              <Button color="primary" type="submit">{passkey.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/passkeys">Cancel</Button>
            </FormGroup>
          </Form>
          </div>
      }
    }

    return <div
    style={{
      marginLeft: marginLeft(expanded),
      padding: '15px 20px 0 20px'
    }}
  >
      <AppNavbar/>
      <Container fluid>
        <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
        {loading(isLoading)}
        {!isLoading && displayContent()}
        <MessageAlert {...displayError}></MessageAlert>
        <FooterContent></FooterContent>
      </Container>
    </div>
  }
}

export default withRouter(PasskeyEdit);