import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {AvFeedback, AvForm, AvGroup, AvInput} from 'availity-reactstrap-validation';
import {Button, Container, Label, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';

class UserEdit extends Component {
  emptyUser = {
    email: '',
    password: '',
    confirmPassword: '',
    authorities: [],
  };

  constructor(props) {
    super(props);
    this.state = {
      user: this.emptyUser,
      displayError: null,
      permissions: [],
      jwt: props.jwt,
      authorities: props.authorities,
      isLoading: true,
      displayAlert: false
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleAuthorityChange = this.handleAuthorityChange.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        return;
      } else {
        if (this.props.match.params.id !== 'new') {
          try {
            const user = await (await fetch(`/api/users/${this.props.match.params.id}`, { method: 'GET',      headers: {
              'Content-Type': 'application/json',
              'Authorization': jwt
            }})).json();
            if (Array.isArray(user.authorities)) {
              user.authorities.forEach((authority, index) => {user.authorities[index] = authority.role});
            }
            this.setState({user: user, isLoading: false});
          } catch (error) {
            this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(error)});
          }
        } else {
          this.setState({isLoading: false});
        }
        try {
          const permissions = await (await fetch('/api/users/permissions', {      headers: {
            'Content-Type': 'application/json',
            'Authorization': jwt
          }})).json();
          this.setState({permissions: permissions});
        } catch (error) {
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(error)});
        }
      }
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let user = {...this.state.user};
    user[name] = value;
    this.setState({user: user});
  }

  handleAuthorityChange(event) {
    const selectedValues = [...event.target.selectedOptions].map(o => o.value);
    console.log("handleAuthorityChange:selectedValues", selectedValues);
    let user = {...this.state.user};
    user.authorities = selectedValues;
    this.setState({user: user});
  }

  async handleSubmit(event) {
    event.preventDefault();
    const {user} = this.state;
    console.log("handleSubmit:user", user);
    const url = '/api/users' + (user.id ? '/' + user.id : '');

    await fetch(url, {
      method: (user.id) ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': this.state.jwt
      },
      body: JSON.stringify(user),
      credentials: 'include'
    }).then(response => response.json())
        .then(data => {
          if (data.id) {
            this.props.history.push('/users');
          } else {
            this.setState({ displayError: errorMessage(data)});
          }
        })
        .catch((error) => {
          console.log(error);
        });
  }

  render() {
    const {user, displayError, permissions, displayAlert, isLoading} = this.state;
    const title = <h2>{user.id ? 'Edit User' : 'Add User'}</h2>;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
                  {title}
        <AvForm onValidSubmit={this.handleSubmit}>
          <AvGroup>
            <Label for="fullName">Full Name</Label>
            <AvInput type="text" name="fullName" id="fullName" value={user.fullName || ''}
                   onChange={this.handleChange} placeholder="Full Name"
                     required
                     minLength="5"
                     maxLength="100"
                     pattern="^[A-Za-z0-9\s+]+$" />
            <AvFeedback>
              This field is invalid - Your Full Name must be between 5 and 100 characters.
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Label for="password">Password</Label>
            <AvInput type="password" name="password" id="password" value={user.password || ''}
                     onChange={this.handleChange} placeholder="Password"
                     required={!user.id} />
            <AvFeedback>
              This field is invalid - Required
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Label for="confirmPassword">Confirm Password</Label>
            <AvInput type="password" name="confirmPassword" id="confirmPassword" value={user.confirmPassword || ''}
                     onChange={this.handleChange} placeholder="Confirm Password"
                     required={!user.id} />
            <AvFeedback>
              This field is invalid - Required
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Label for="name">Email</Label>
            <AvInput type="email" name="email" id="email" value={user.email || ''}
                     required
                     onChange={this.handleChange} placeholder="your_email@email.com"/>
            <AvFeedback>
              This field is invalid - Please enter a correct email.
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Label for="authorities">Permissions</Label>
            <AvInput type="select" name="authorities" id="authorities" value={user.authorities || ''}
                     required
                     multiple
                     onChange={this.handleAuthorityChange}>
              {permissions.map(role => <option value={role} key={role}>{role}</option>)}
            </AvInput>
            <AvFeedback>
              This field is invalid - Please select at least one permission.
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Button color="primary" type="submit">{user.id ? 'Save' : 'Create'}</Button>{' '}
            <Button color="secondary" tag={Link} to="/users">Cancel</Button>
          </AvGroup>
        </AvForm>
        </div>
      }
    }

    return (
    <div>
      <AppNavbar/>
      <Container fluid>
      <HomeContent notDisplayMessage={true}></HomeContent>
      {displayContent()}
      <MessageAlert {...displayError}></MessageAlert>
      <FooterContent></FooterContent>
      </Container>
    </div>
    )
  }
}

export default withRouter(UserEdit);