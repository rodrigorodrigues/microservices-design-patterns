import React, {Component} from 'react';
import {Link} from 'react-router-dom';
import {AvFeedback, AvForm, AvGroup, AvInput} from 'availity-reactstrap-validation';
import {Button, Container, Label, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';
import { marginLeft } from '../common/Util';
import withRouter from '../common/WithRouter';

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
    event.preventDefault();
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
        'Authorization': jwt
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
          <AvForm onValidSubmit={this.handleSubmit}>
            <AvGroup>
              <Label for="name">Name</Label>
              <AvInput type="text" name="name" id="name" value={product.name || ''}
                    onChange={this.handleChange} placeholder="Name"
                      required/>
              <AvFeedback>
                This field is invalid
              </AvFeedback>
            </AvGroup>
            <AvGroup>
              <Label for="category">Category</Label>
              <AvInput type="text" name="category" id="category" value={product.category || ''}
                    onChange={this.handleChange} placeholder="Category" required />
              <AvFeedback>
                This field is invalid
              </AvFeedback>
            </AvGroup>
            <AvGroup>
              <Label for="quantity">Quantity</Label>
              <AvInput type="number" name="quantity" id="quantity" value={product.quantity || ''}
                    onChange={this.handleChange} placeholder="Quantity" required />
              <AvFeedback>
                This field is invalid
              </AvFeedback>
            </AvGroup>
            <AvGroup>
              <Button color="primary" type="submit">{product._id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/products">Cancel</Button>
            </AvGroup>
          </AvForm>
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