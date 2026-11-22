import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {Feedback, Form, FormGroup, Input, Label} from '@availity/form';
import {Button, Container, UncontrolledAlert} from 'reactstrap';
import * as yup from 'yup';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import FooterContent from '../home/FooterContent';
import HomeContent from '../home/HomeContent';
import { marginLeft } from '../common/Util';
import { loading } from '../common/Loading';
import uuid from 'react-uuid';

// dotnet-service expects numbers for quantity and price.
export function toStockPayload(stock) {
  return {
    name: stock.name,
    quantity: Number(stock.quantity),
    category: stock.category,
    price: Number(stock.price),
    currency: stock.currency ? stock.currency.toUpperCase() : stock.currency
  };
}

// dotnet-service answers 403 with an empty body; validation errors come as JSON (ASP.NET problem details).
export function parseResponseBody(text, response) {
  if (!text) {
    return { error: `${response.status} - ${response.statusText}` };
  }
  try {
    return JSON.parse(text);
  } catch (e) {
    return { error: text };
  }
}

class StockEdit extends Component {
  emptyStock = {
    name: '',
    quantity: '',
    category: '',
    price: '',
    currency: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      stock: this.emptyStock,
      jwt: props.jwt,
      displayError: null,
      displayAlert: false,
      authorities: props.authorities,
      isLoading: false,
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
    try {
      this.setLoading(true);
      const { jwt, authorities, gatewayUrl } = this.state;
      if (jwt && authorities) {
        if (!authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_STOCKS_CREATE' || item === 'ROLE_STOCKS_SAVE' || item === 'SCOPE_openid')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          if (this.props.match.params.id !== 'new') {
            try {
              const response = await fetch(`${gatewayUrl}/api/stocks/${this.props.match.params.id}`, { method: 'GET', headers: {
                'Content-Type': 'application/json',
                'Authorization': jwt
              }});
              const stock = parseResponseBody(await response.text(), response);
              if (response.status === 200) {
                this.setState({stock: stock, isLoading: false});
              } else {
                this.setState({displayAlert: response.status === 401, isLoading: false, displayError: errorMessage(stock)});
              }
            } catch (error) {
              this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(error)});
            }
          } else {
            this.setState({isLoading: false});
          }
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let stock = {...this.state.stock};
    stock[name] = value;
    this.setState({stock: stock});
  }

  async handleSubmit(event) {
    try {
      const {stock, jwt, gatewayUrl} = this.state;

      const url = `${gatewayUrl}/api/stocks` + (stock.id ? '/' + stock.id : '');
      await fetch(url, {
        method: (stock.id) ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': jwt,
          'requestId': uuid()
        },
        body: JSON.stringify(toStockPayload(stock)),
        credentials: 'include'
      }).then(async response => parseResponseBody(await response.text(), response))
          .then(data => {
            if (data.id) {
              this.props.history.push('/stocks');
            } else {
              this.setState({ displayError: errorMessage(data)});
            }
          })
          .catch((error) => {
            this.setState({ displayError: errorMessage(error)});
          });
    } finally {
      this.setLoading(false);
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  render() {
    const { stock, displayError, displayAlert, isLoading, expanded } = this.state;
    const title = <h2>{stock.id ? 'Edit Stock' : 'Add Stock'}</h2>;

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
              name: stock?.name || '',
              quantity: stock?.quantity === 0 ? 0 : (stock?.quantity || ''),
              category: stock?.category || '',
              price: stock?.price === 0 ? 0 : (stock?.price || ''),
              currency: stock?.currency || ''
            }}
            validationSchema={yup.object().shape({
              name: yup.string().trim().max(255).required('This field is required.'),
              quantity: yup.number().integer().min(0).required('This field is required.'),
              category: yup.string().trim().max(255).required('This field is required.'),
              price: yup.number().min(0).required('This field is required.'),
              currency: yup.string().trim().length(3, 'Use a 3-letter currency code.').required('This field is required.')
            })}
          >
            <FormGroup>
              <Label for="name">Name</Label>
              <Input type="text" name="name" id="name" value={stock.name || ''}
                    onChange={this.handleChange} placeholder="Name"
                      required/>
              <Feedback name="name">
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="category">Category</Label>
              <Input type="text" name="category" id="category" value={stock.category || ''}
                    onChange={this.handleChange} placeholder="Category" required/>
              <Feedback name="category">
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="quantity">Quantity</Label>
              <Input type="number" step="1" name="quantity" id="quantity" value={stock.quantity === 0 ? 0 : (stock.quantity || '')}
                    onChange={this.handleChange} placeholder="Quantity" required/>
              <Feedback name="quantity">
                Enter a whole number, 0 or more
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="price">Price</Label>
              <Input type="number" step="0.01" name="price" id="price" value={stock.price === 0 ? 0 : (stock.price || '')}
                    onChange={this.handleChange} placeholder="Price" required/>
              <Feedback name="price">
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="currency">Currency</Label>
              <Input type="text" name="currency" id="currency" value={stock.currency || ''}
                    onChange={this.handleChange} placeholder="USD" maxLength={3} required/>
              <Feedback name="currency">
                Use a 3-letter currency code
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Button color="primary" type="submit">{stock.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/stocks">Cancel</Button>
            </FormGroup>
          </Form>
          </div>
      }
    }

    return <div
    style={{
      marginLeft: marginLeft(expanded),
      padding: '15px 20px 0 20px'
    }}
  >
      <AppNavbar/>
      <Container fluid>
        <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
        {loading(isLoading)}
        {!isLoading && displayContent()}
        <MessageAlert {...displayError}></MessageAlert>
        <FooterContent></FooterContent>
      </Container>
    </div>
  }
}

export default withRouter(StockEdit);
