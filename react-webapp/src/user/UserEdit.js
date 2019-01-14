import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {AvFeedback, AvForm, AvGroup, AvInput} from 'availity-reactstrap-validation';
import {Button, Container, Label} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';

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
      displayError: null
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  async componentDidMount() {
    if (this.props.match.params.id !== 'new') {
      try {
        const user = await (await fetch(`/api/users/${this.props.match.params.id}`)).json();
        user.authorities.forEach((authority, index) => {user.authorities[index] = authority.role});
        this.setState({user: user});
      } catch (error) {
        this.setState({ displayError: errorMessage(error)});
      }
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let person = {...this.state.user};
    person[name] = value;
    this.setState({user: person});
  }

  async handleSubmit(event) {
    event.preventDefault();
    const {user} = this.state;

    await fetch('/api/users', {
      method: (user.id) ? 'PUT' : 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(person),
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
    const {user, displayError} = this.state;
    const title = <h2>{user.id ? 'Edit User' : 'Add User'}</h2>;

    return <div>
      <AppNavbar/>
      <Container>
        {title}
        <AvForm onValidSubmit={this.handleSubmit}>
          <AvGroup>
            <Label for="name">Full Name</Label>
            <AvInput type="text" name="name" id="name" value={user.name || ''}
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
                     required />
            <AvFeedback>
              This field is invalid - Required
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Label for="confirmPassword">Confirm Password</Label>
            <AvInput type="password" name="confirmPassword" id="confirmPassword" value={user.confirmPassword || ''}
                     onChange={this.handleChange} placeholder="Confirm Password"
                     required />
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
                     onChange={this.handleChange}>
              {['ROLE_ADMIN', 'ROLE_CREATE', 'ROLE_READ', 'ROLE_SAVE', 'ROLE_DELETE']
                  .map(role => <option value={role} key={role}>{role}</option>)}
            </AvInput>
            <AvFeedback>
              This field is invalid - Please select at least one permission.
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Button color="primary" type="submit">{user.id ? 'Save' : 'Create'}</Button>{' '}
            <Button color="secondary" tag={Link} to="/persons">Cancel</Button>
          </AvGroup>
          <MessageAlert {...displayError}></MessageAlert>
        </AvForm>
      </Container>
    </div>
  }
}

export default withRouter(UserEdit);