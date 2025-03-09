import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {Feedback, Form, FormGroup, Input} from '@availity/form';
import * as yup from 'yup';
import {Button, Container, Label, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';
import { marginLeft } from '../common/Util';
import uuid from 'react-uuid';

class CategoryEdit extends Component {
  emptyCategory = {
    name: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      category: this.emptyCategory,
      displayError: null,
      jwt: props.jwt,
      authorities: props.authorities,
      isLoading: true,
      displayAlert: false,
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

  async componentDidMount() {
    const { jwt, authorities, gatewayUrl } = this.state;
    if (jwt && authorities) {
      if (!authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_CATEGORY_CREATE' || item === 'ROLE_CATEGORY_SAVE' || item === 'SCOPE_openid')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        if (this.props.match.params.id !== 'new') {
          try {
            const category = await (await fetch(`${gatewayUrl}/api/week-menu/v2/category/${this.props.match.params.id}`, { method: 'GET',      headers: {
              'Content-Type': 'application/json',
              'Authorization': jwt
            }})).json();
            this.setState({isLoading: false, category: category});
          } catch (error) {
            this.setState({ displayAlert: true, displayError: errorMessage(error)});
          }
        } else {
          this.setState({isLoading: false});
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
    const { category, jwt, gatewayUrl } = this.state;

    await fetch(`${gatewayUrl}/api/week-menu/v2/category`, {
      method: (category._id) ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': jwt,
        'requestId': uuid()
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
    const { category, displayError, displayAlert, isLoading, expanded } = this.state;
    const title = <h2>{category._id ? 'Edit Category' : 'Add Category'}</h2>;

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
              name: category?.name || '',
              products: category?.products || ''
            }}
            validationSchema={yup.object().shape({
              name: yup.string().trim().required('This field is required.'),
              products: yup.string().trim().required('This field is required.')
            })}
          >
            <FormGroup>
              <Label for="name">Name</Label>
              <Input type="text" name="name" id="name" value={category.name || ''}
                    onChange={this.handleChange} placeholder="Name"
                      required/>
              <Feedback>
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="products">Products</Label>
              <Input type="text" name="products" id="products" value={category.products || ''}
                    onChange={this.handleChange} placeholder="Products" />
              <Feedback>
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Button color="primary" type="submit">{category._id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/categories">Cancel</Button>
            </FormGroup>
          </Form>
        </div>
      }
    }

    return (
      <div
        style={{
          marginLeft: marginLeft(expanded),
          padding: '15px 20px 0 20px'
      }}
      >
      <AppNavbar/>
      <Container fluid>
        <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
        {isLoading && <i class="fa fa-spinner" aria-hidden="true"></i>}
        {!isLoading && displayContent()}
        <MessageAlert {...displayError}></MessageAlert>
        <FooterContent></FooterContent>
      </Container>
    </div>)
  }
}

export default withRouter(CategoryEdit);