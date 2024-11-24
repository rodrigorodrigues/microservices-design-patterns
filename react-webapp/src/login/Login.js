import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import {Feedback, Form, FormGroup, Input, Label} from '@availity/form';
import * as yup from 'yup';
import { Row, Col,  Button, Container, Card, CardBody, CardTitle, CardSubtitle, CardText, CardLink } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { postWithHeaders, postWithHeadersAndApi, getWithCredentials } from '../services/ApiService';
import MessageAlert from '../MessageAlert';
import { errorMessage, marginLeft } from '../common/Util';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';
import HomeContent from '../home/HomeContent';
import { loading } from '../common/Loading';
import uuid from 'react-uuid';
const {client, parsers} = require('@passwordless-id/webauthn');

const googleOauthUrl = process.env.REACT_APP_GOOGLE_OAUTH_URL;
console.log("googleOauthUrl: " + googleOauthUrl);

class Login extends Component {
  constructor(props) {
    super(props);
    this.state = {
      login: {
        username: '',
        password: ''
      },
      otpUsername: '',
      isLoading: false,
      displayError: null,
      isAuthenticated: props.isAuthenticated,
      expanded: false
    };
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handlePasskey = this.handlePasskey.bind(this);
    this.handleOtp = this.handleOtp.bind(this);
  }

  async componentDidMount() {
    toast.dismiss('Error');
    if (this.state.isAuthenticated) {
      if (window.localStorage.getItem('redirectToPreviousPage') !== null) {
        window.localStorage.removeItem('redirectToPreviousPage');
        window.location = window.localStorage.getItem('redirectToPreviousPage');
      } else {
        this.props.history.push('/');
      }
    }
  }

  setExpanded = (expanded) => {
    this.setState({ expanded: expanded });
  }

  validateForm = () => {
    const { username, password } = this.state.login;
    return username.trim().length > 0 && password.length > 0;
  }

  validateOtpForm = () => {
    const { otpUsername } = this.state;
    return otpUsername.trim().length > 0;
  }


  async handlePasskey() {
    try {
      const { setAuthentication, history } = this.props;
      this.setLoading(true);
      console.log(client);

      let csrfData = await getWithCredentials('csrf', false);
      const csrfToken = csrfData.token;

      // 1. Get a challenge from the server
      const response = await postWithHeadersAndApi('webauthn/authenticate/options', null, 
        { 
          'Content-Type': 'application/json', 
          'X-XSRF-TOKEN': csrfToken,
          'requestId': uuid() 
        }, true);
      // 2. Invoking WebAuthn in the browser
      const authentication = await client.authenticate({
        challenge: response?.challenge,
        userVerification: response?.userVerification,
        timeout: response?.timeout
      });

      // 3. Send the payload to the server
      console.log('Authentication payload: ' + JSON.stringify(authentication, null, 2));

      const responseLoginWebauthn = await postWithHeadersAndApi('login/webauthn', JSON.stringify(authentication), 
        { 
          'Content-Type': 'application/json', 
          'X-XSRF-TOKEN': csrfToken,
          'requestId': uuid() 
        }, true);

      // 4. The server can now verify the payload, but let's just parse it for the demo
      console.log(`authenticationParsed: ${JSON.stringify(responseLoginWebauthn, null, 2)}`);
      const data = await getWithCredentials('authenticatedUser', false);
      if (data.tokenValue) {
        window.localStorage.removeItem('redirectToPreviousPage');
        setAuthentication(data);
        const redirectToPreviousPage = window.localStorage.getItem('redirectToPreviousPage');
        if (redirectToPreviousPage !== null) {
          window.localStorage.removeItem('redirectToPreviousPage');
          window.location = redirectToPreviousPage;
        } else {
          history.push('/');
        }
      } else {
        this.setState({ displayError: errorMessage(data) });
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    } finally {
      this.setLoading(false);
    }
  }

  async handleOtp() {
    const { otpUsername } = this.state;
    const otpSubmit = "username=" + encodeURIComponent(otpUsername);

    try {
      this.setLoading(true);

      let csrfData = await getWithCredentials('csrf', false);
      const csrfToken = csrfData.token;

      const data = await postWithHeadersAndApi('ott/generate', otpSubmit, 
      { 
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 
        'X-XSRF-TOKEN': csrfToken,
        'requestId': uuid() 
      }, true);
      this.setState({ displayError: errorMessage(data, 'success') });
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    } finally {
      this.setLoading(false);
    }
  }

  async handleSubmit(event) {
    const { login } = this.state;
    const { setAuthentication, history } = this.props;
    const loginSubmit = "username=" + encodeURIComponent(login.username) + '&password=' + encodeURIComponent(login.password);

    try {
      this.setLoading(true);

      let csrfData = await getWithCredentials('csrf', false);
      const csrfToken = csrfData.token;

      const redirectToPreviousPage = window.localStorage.getItem('redirectToPreviousPage');
      console.log("redirectToPreviousPage: ", redirectToPreviousPage);
      const data = await postWithHeaders('authenticate', loginSubmit, 
        { 
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 
          'X-XSRF-TOKEN': csrfToken,
          'requestId': uuid() 
        });
      if (data.tokenValue) {
        setAuthentication(data);
        if (redirectToPreviousPage !== null) {
          window.localStorage.removeItem('redirectToPreviousPage');
          window.location = redirectToPreviousPage;
        } else {
          history.push('/');
        }
      } else {
        this.setState({ displayError: errorMessage(data) });
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    } finally {
      this.setLoading(false);
    }
  }

  handleChange = (event) => {
    const { target } = event;
    const { login } = this.state;
    login[target.name] = target.value;
    this.setState({ login });
  }

  handleChangeOtp = (event) => {
    const { target } = event;
    this.setState({ otpUsername: target.value });
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  render() {
    const { login, displayError, expanded, isLoading } = this.state;

    return <div
      style={{
        marginLeft: marginLeft(expanded),
        padding: '15px 20px 0 20px'
      }}
    >
      <AppNavbar />
      <Container fluid>
        <HomeContent setExpanded={this.setExpanded} {...this.state} />
          <Row>
          <Col>
            <Form onSubmit={this.handleSubmit}
              enableReinitialize={true}
              initialValues={{
                username: login['username'],
                password: login['password']
              }}
              validationSchema={yup.object({
                username: yup.string().trim().required(),
                password: yup.string().trim().required()
              })}
            >
              <Card>
                <CardBody>
                  <CardTitle tag="h5">
                    Form Login
                  </CardTitle>
                  <CardSubtitle
                    className="mb-2 text-muted"
                    tag="h6"
                  >
                    <FormGroup>
                      <Label for="username">Username</Label>
                      <Input type="text" name="username" id="username" value={login['username']}
                            onChange={this.handleChange} placeholder="Username"
                              />
                      <Feedback name="username" />
                    </FormGroup>
                  </CardSubtitle>
                  <CardText>
                    <FormGroup>
                      <Label for="password">Password</Label>
                      <Input type="text" name="password" id="password" type="password" value={login['password']}
                            onChange={this.handleChange} placeholder="Password"
                              />
                      <Feedback name="password" />
                    </FormGroup>
                  </CardText>
                  <FormGroup>
                    <Button color="primary" block
                      disabled={!this.validateForm()}
                      className={this.validateForm() === false ? 'disabled' : ''}
                      type="submit">
                      Sign in
                    </Button>
                  </FormGroup>
                </CardBody>
              </Card>      
              </Form>
            </Col>
            <Col>
              <Form onSubmit={this.handleOtp}
                enableReinitialize={true}
                initialValues={{
                  otpUsername: login['username']
                }}
                validationSchema={yup.object().shape({
                  otpUsername: yup.string().trim().required('This field is invalid')
                })}
              >
              <Card>
                <CardBody>
                  <CardTitle tag="h5">
                    One Time Token Login
                  </CardTitle>
                  <CardSubtitle
                    className="mb-2 text-muted"
                    tag="h6"
                  >
                    <FormGroup>
                      <Label for="otpUsername">Username</Label>
                      <Input type="text" name="otpUsername" id="otpUsername" 
                            onChange={this.handleChangeOtp} placeholder="Username"
                              />
                      <Feedback name="otpUsername" />
                    </FormGroup>
                  </CardSubtitle>
                  <CardText>
                    <FormGroup>
                      <Button color="primary" block
                        disabled={!this.validateOtpForm()}
                        className={this.validateOtpForm() === false ? 'disabled' : ''}
                        type="submit">
                        Send Token
                      </Button>
                    </FormGroup>
                  </CardText>
                </CardBody>
              </Card>
              </Form>
            </Col>
          </Row>
          <Row>
            <Col>
              <Card>
                <CardBody>
                  <CardTitle tag="h5">
                    Social Login
                  </CardTitle>
                  <CardSubtitle
                    className="mb-2 text-muted"
                    tag="h6"
                  >
                      <Button color="primary" block type="button" onClick={() => window.location.href = `${googleOauthUrl}`}>
                        <i className="fa fa-fw fa-google" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} /> Google
                      </Button>
                  </CardSubtitle>
                </CardBody>
              </Card>
            </Col>
            <Col>
              <Card>
                <CardBody>
                  <CardTitle tag="h5">
                    Passkeys Login
                  </CardTitle>
                  <CardSubtitle
                    className="mb-2 text-muted"
                    tag="h6"
                  >
                      <Button color="primary" block type="button" onClick={() => this.handlePasskey()}>
                        <i className="fa fa-fw" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} /> Sign in with a passkey
                      </Button>
                  </CardSubtitle>
                </CardBody>
              </Card>
            </Col>
          </Row>
          {loading(isLoading)}
          <MessageAlert {...displayError}></MessageAlert>
        <FooterContent></FooterContent>
      </Container>
    </div>
  }
}
export default withRouter(Login);