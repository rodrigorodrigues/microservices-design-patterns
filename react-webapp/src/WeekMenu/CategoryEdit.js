import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {AvFeedback, AvForm, AvGroup, AvInput} from 'availity-reactstrap-validation';
import {Button, Container, Label} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';

class CategoryEdit extends Component {
  emptyCategory = {
    name: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      category: this.emptyCategory,
      displayError: null,
      jwt: props.jwt
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    if (jwt) {
      if (this.props.match.params.id !== 'new') {
        try {
          const category = await (await fetch(`/api/week-menu/v2/category/${this.props.match.params.id}`, { method: 'GET',      headers: {
            'Content-Type': 'application/json',
            'Authorization': jwt
          }})).json();
          this.setState({category: category});
        } catch (error) {
          this.setState({ displayError: errorMessage(error)});
        }
      }
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let category = {...this.state.category};
    category[name] = value;
    this.setState({category: category});
  }

  async handleSubmit(event) {
    event.preventDefault();
    const {category, jwt} = this.state;

    await fetch('/api/week-menu/v2/category', {
      method: (category._id) ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': jwt
      },
      body: JSON.stringify(category),
      credentials: 'include'
    }).then(response => {
      console.log("Data json response: ", response);
      return response.json();
    })
        .then(data => {
          console.log("Data response: ", data);
          if (data._id) {
            this.props.history.push('/categories');
          } else {
            this.setState({ displayError: errorMessage(data)});
          }
        })
        .catch((error) => {
          console.log(error);
        });
  }

  render() {
    const {category, displayError} = this.state;
    const title = <h2>{category._id ? 'Edit Category' : 'Add Category'}</h2>;

    return <div>
      <AppNavbar/>
      <Container>
        {title}
        <AvForm onValidSubmit={this.handleSubmit}>
          <AvGroup>
            <Label for="name">Name</Label>
            <AvInput type="text" name="name" id="name" value={category.name || ''}
                   onChange={this.handleChange} placeholder="Name"
                     required/>
            <AvFeedback>
              This field is invalid
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Label for="products">Products</Label>
            <AvInput type="text" name="products" id="products" value={category.products || ''}
                   onChange={this.handleChange} placeholder="Products" />
            <AvFeedback>
              This field is invalid
            </AvFeedback>
          </AvGroup>
          <AvGroup>
            <Button color="primary" type="submit">{category._id ? 'Save' : 'Create'}</Button>{' '}
            <Button color="secondary" tag={Link} to="/categories">Cancel</Button>
          </AvGroup>
          <MessageAlert {...displayError}></MessageAlert>
        </AvForm>
      </Container>
    </div>
  }
}

export default withRouter(CategoryEdit);