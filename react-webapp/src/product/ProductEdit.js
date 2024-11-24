import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {Feedback, Form, FormGroup, Input, Label} from '@availity/form';
import * as yup from 'yup';
import {Button, Container, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';
import { marginLeft } from '../common/Util';
import uuid from 'react-uuid';

class ProductEdit extends Component {
  emptyProduct = {
    name: '',
    category: '',
    quantity: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      product: this.emptyProduct,
      displayError: null,
      jwt: props.jwt,
      authorities: props.authorities,
      isLoading: true,
      displayAlert: false,
      expanded: false,
      isAuthenticated: props.isAuthenticated
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PRODUCT_CREATE' || item === 'ROLE_PRODUCT_SAVE' || item === 'SCOPE_openid')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        if (this.props.match.params.id !== 'new') {
          try {
            const product = await (await fetch(`/api/products/${this.props.match.params.id}`, { method: 'GET',      headers: {
              'Content-Type': 'application/json',
              'Authorization': jwt
            }})).json();
            this.setState({isLoading: false, product: product});
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
    let product = {...this.state.product};
    product[name] = value;
    this.setState({product: product});
  }

  async handleSubmit(event) {
    const {product, jwt} = this.state;
    const productCopy = {
      name: product.name,
      category: product.category,
      quantity: product.quantity
    };

    await fetch(`/api/products${product._id ? '/' + product._id.$oid : ''}`, {
      method: (product._id ? 'PUT' : 'POST'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': jwt,
        'requestId': uuid()
      },
      body: JSON.stringify(productCopy),
      credentials: 'include'
    }).then(response => {
      console.log("Data json response: ", response);
      return response.json();
    })
        .then(data => {
          console.log("Data response: ", data);
          if (data._id) {
            this.props.history.push('/products');
          } else {
            this.setState({ displayError: errorMessage(data)});
          }
        })
        .catch((error) => {
          console.log(error);
        });
  }

  render() {
    const { product, displayError, displayAlert, isLoading, expanded } = this.state;
    const title = <h2>{product._id ? 'Edit Product' : 'Add Product'}</h2>;

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
              name: product?.name || '',
              category: product?.catch || '',
              quantity: product?.quantity || '',
              currency: product?.currency || '',
              price: product?.price || ''
            }}
            validationSchema={yup.object().shape({
              name: yup.string().trim().required('This field is required.'),
              category: yup.string().trim().required('This field is required.'),
              quantity: yup.number().positive().required('This field is required.'),
              currency: yup.string().trim().required('This field is required.'),
              price: yup.number().positive().required('This field is required.')
            })}
          >
            <FormGroup>
              <Label for="name">Name</Label>
              <Input type="text" name="name" id="name" value={product.name || ''}
                    onChange={this.handleChange} placeholder="Name"
                      required/>
              <Feedback>
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="category">Category</Label>
              <Input type="text" name="category" id="category" value={product.category || ''}
                    onChange={this.handleChange} placeholder="Category" required />
              <Feedback>
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="quantity">Quantity</Label>
              <Input type="number" name="quantity" id="quantity" value={product.quantity || ''}
                    onChange={this.handleChange} placeholder="Quantity" required />
              <Feedback>
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="price">Price</Label>
              <Input type="number" name="price" id="price" value={product.price || ''}
                    onChange={this.handleChange} placeholder="Price" required />
              <Feedback>
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="currency">Currency</Label>
              <Input type="text" name="currency" id="currency" value={product.currency || ''}
                    onChange={this.handleChange} placeholder="Currency" required />
              <Feedback>
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Button color="primary" type="submit">{product._id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/products">Cancel</Button>
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
    </div>);
  }
}

export default withRouter(ProductEdit);