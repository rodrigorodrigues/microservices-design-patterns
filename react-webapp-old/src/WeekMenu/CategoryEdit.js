import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {AvFeedback, AvForm, AvGroup, AvInput} from 'availity-reactstrap-validation';
import {Button, Container, Label} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';

class CategoryEdit extends Component {
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
        const user = await (await fetch(`/api/week-menu/v2/category/${this.props.match.params.id}`)).json();
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
    const {user, jwt} = this.state;

    await fetch('/api/week-menu/v2/category', {
      method: (user.id) ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': jwt
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
    const {user, displayError} = this.state;
    const title = <h2>{user.id ? 'Edit Category' : 'Add Category'}</h2>;

    return <div>
      <AppNavbar/>
      <Container>
        {title}
        <AvForm onValidSubmit={this.handleSubmit}>
          <AvGroup>
            <Label for="name">Name</Label>
            <AvInput type="text" name="name" id="name" value={user.name || ''}
                   onChange={this.handleChange} placeholder="Name"
                     required/>
            <AvFeedback>
              This field is invalid
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Label for="products">Products</Label>
            <AvInput type="text" name="products" id="products" value={user.products || ''}
                   onChange={this.handleChange} placeholder="Products"
                     required/>
            <AvFeedback>
              This field is invalid
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Button color="primary" type="submit">{user.id ? 'Save' : 'Create'}</Button>{' '}
            <Button color="secondary" tag={Link} to="/categories">Cancel</Button>
          </AvGroup>
          <MessageAlert {...displayError}></MessageAlert>
        </AvForm>
      </Container>
    </div>
  }
}

export default withRouter(CategoryEdit);