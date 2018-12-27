import React, {Component} from 'react';
import {withRouter} from 'react-router-dom';
import {Button, Container, Form, FormGroup, Input, Label} from 'reactstrap';
import AppNavbar from './AppNavbar';
import {connect} from 'react-redux';
import PropTypes from 'prop-types';

class Login extends Component {
  static propTypes = {
    jwt: PropTypes.string.isRequired
  };

  login = {
    username: '',
    password: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      item: this.login
    };
    this.validateForm = this.validateForm.bind();
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  validateForm() {
    return this.login.username.length > 0 && this.login.password.length > 0;
  }

  async handleSubmit(event) {
    event.preventDefault();
    const {login} = this.state;

    await fetch('/user/authenticate', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(login)
    });
    this.props.history.push('/');
  }

  render() {
    const {login} = this.state;

    return <div>
      <AppNavbar/>
      <Container>
        <h2>Login</h2>
        <Form onSubmit={this.handleSubmit}>
          <FormGroup>
            <Label for="name">Username</Label>
            <Input type="text" name="username" id="username" value={login.username || ''}/>
          </FormGroup>
          <FormGroup>
            <Label for="age">Password</Label>
            <Input type="text" name="password" id="password" value={item.password || ''} />
          </FormGroup>
          <FormGroup>
            <Button color="primary" block
                    bsSize="large"
                    disabled={!this.validateForm}
                    type="submit">Submit</Button>
          </FormGroup>
        </Form>
      </Container>
    </div>
  }
}

const mapStateToProps = state => ({
  jwt: state.propTypes.jwt,
});

export default connect(mapStateToProps)(withRouter(Login));