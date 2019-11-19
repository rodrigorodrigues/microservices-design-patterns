import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import { Button, Container, Form, FormGroup, Input, Label } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { postWithHeaders } from '../services/ApiService';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import Cookies from 'js-cookie';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';

class Login extends Component {
  constructor(props) {
    super(props);
    this.state = {
      login:  {
        username: '',
        password: ''
      },
      displayError: null,
      isAuthenticated: props.isAuthenticated
    };
    console.log("state: ", this.state);
    this.handleSubmit = this.handleSubmit.bind(this);
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

  validateForm = () => {
    const { username, password } = this.state.login;
    return username.length > 0 && password.length > 0;
  }

  async handleSubmit(event) {
    event.preventDefault();
    const { login } = this.state;
    const { setAuthentication, history } = this.props;
    const loginSubmit = "username=" + encodeURIComponent(login.username) + '&password=' + encodeURIComponent(login.password);

    try {
      const redirectToPreviousPage = window.localStorage.getItem('redirectToPreviousPage');
      console.log("redirectToPreviousPage: ", redirectToPreviousPage);
      const data = await postWithHeaders('authenticate', loginSubmit, {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 'X-XSRF-TOKEN': Cookies.get('XSRF-TOKEN')});
      if (data.id_token) {
        setAuthentication(data);
        if (redirectToPreviousPage !== null) {
          window.localStorage.removeItem('redirectToPreviousPage');
          window.location = redirectToPreviousPage;
        } else {
          history.push('/');
        }
      } else {
        this.setState({ displayError: errorMessage(data)});
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error)});
    }
  }

  handleChange = (event) => {
    const { target } = event;
    const { login } = this.state;
    login[target.name] = target.value;
    this.setState({ login });
  }

  render() {
    const { login, displayError } = this.state;

    return <div>
      <AppNavbar />
      <Container>
        <h2>Login</h2>
        <Form onSubmit={this.handleSubmit}>
          <FormGroup className="col-md-3 mb-3">
            <Label for="name">Username</Label>
            <Input 
              type="text" 
              name="username" 
              id="username" 
              onChange={this.handleChange} 
              value={login['username']} />
          </FormGroup>
          <FormGroup className="col-md-3 mb-3">
            <Label for="password">Password</Label>
            <Input type="password" 
              name="password" 
              id="password" 
              onChange={this.handleChange} 
              value={login['password']} />
          </FormGroup>
          <FormGroup className="col-md-3 mb-3">
            <Button color="primary" block
              disabled={!this.validateForm()}
              className={this.validateForm() === false ? 'disabled' : ''}
              type="submit">
              Submit
            </Button>
          </FormGroup>
          <MessageAlert {...displayError}></MessageAlert>
        </Form>
        <FooterContent></FooterContent>
      </Container>
    </div>
  }
}
export default withRouter(Login);