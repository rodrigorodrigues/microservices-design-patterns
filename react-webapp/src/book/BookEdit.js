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

// Same rule php-service applies: ISBN-10 or ISBN-13, hyphens allowed.
export const ISBN_PATTERN = /^\d[\d-]{8,15}[\dXx]$/;

// php-service expects numbers for publishedYear and price.
export function toBookPayload(book) {
  return {
    title: book.title,
    author: book.author,
    isbn: book.isbn,
    publishedYear: Number(book.publishedYear),
    price: Number(book.price),
    currency: book.currency ? book.currency.toUpperCase() : book.currency
  };
}

class BookEdit extends Component {
  emptyBook = {
    title: '',
    author: '',
    isbn: '',
    publishedYear: '',
    price: '',
    currency: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      book: this.emptyBook,
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
        if (!authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_BOOKS_CREATE' || item === 'ROLE_BOOKS_SAVE' || item === 'SCOPE_openid')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          if (this.props.match.params.id !== 'new') {
            try {
              const response = await fetch(`${gatewayUrl}/api/books/${this.props.match.params.id}`, { method: 'GET', headers: {
                'Content-Type': 'application/json',
                'Authorization': jwt
              }});
              const book = await response.json();
              if (response.status === 200) {
                this.setState({book: book, isLoading: false});
              } else {
                this.setState({displayAlert: response.status === 401, isLoading: false, displayError: errorMessage(book)});
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
    let book = {...this.state.book};
    book[name] = value;
    this.setState({book: book});
  }

  async handleSubmit(event) {
    try {
      const {book, jwt, gatewayUrl} = this.state;

      const url = `${gatewayUrl}/api/books` + (book.id ? '/' + book.id : '');
      await fetch(url, {
        method: (book.id) ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': jwt,
          'requestId': uuid()
        },
        body: JSON.stringify(toBookPayload(book)),
        credentials: 'include'
      }).then(response => response.json())
          .then(data => {
            if (data.id) {
              this.props.history.push('/books');
            } else {
              // 409 duplicate ISBN or 422 validation errors from php-service.
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
    const { book, displayError, displayAlert, isLoading, expanded } = this.state;
    const title = <h2>{book.id ? 'Edit Book' : 'Add Book'}</h2>;
    const maxYear = new Date().getFullYear() + 1;

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
              title: book?.title || '',
              author: book?.author || '',
              isbn: book?.isbn || '',
              publishedYear: book?.publishedYear || '',
              price: book?.price || '',
              currency: book?.currency || ''
            }}
            validationSchema={yup.object().shape({
              title: yup.string().trim().max(255).required('This field is required.'),
              author: yup.string().trim().max(255).required('This field is required.'),
              isbn: yup.string().trim().matches(ISBN_PATTERN, 'Enter an ISBN-10 or ISBN-13.').required('This field is required.'),
              publishedYear: yup.number().integer().min(0).max(maxYear).required('This field is required.'),
              price: yup.number().min(0).required('This field is required.'),
              currency: yup.string().trim().length(3, 'Use a 3-letter currency code.').required('This field is required.')
            })}
          >
            <FormGroup>
              <Label for="title">Title</Label>
              <Input type="text" name="title" id="title" value={book.title || ''}
                    onChange={this.handleChange} placeholder="Title"
                      required/>
              <Feedback name="title">
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="author">Author</Label>
              <Input type="text" name="author" id="author" value={book.author || ''}
                    onChange={this.handleChange} placeholder="Author" required/>
              <Feedback name="author">
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="isbn">ISBN</Label>
              <Input type="text" name="isbn" id="isbn" value={book.isbn || ''}
                    onChange={this.handleChange} placeholder="978-0321125217" required/>
              <Feedback name="isbn">
                Enter an ISBN-10 or ISBN-13
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="publishedYear">Published Year</Label>
              <Input type="number" name="publishedYear" id="publishedYear" value={book.publishedYear || ''}
                    onChange={this.handleChange} placeholder="Published Year" required/>
              <Feedback name="publishedYear">
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="price">Price</Label>
              <Input type="number" step="0.01" name="price" id="price" value={book.price === 0 ? 0 : (book.price || '')}
                    onChange={this.handleChange} placeholder="Price" required/>
              <Feedback name="price">
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="currency">Currency</Label>
              <Input type="text" name="currency" id="currency" value={book.currency || ''}
                    onChange={this.handleChange} placeholder="USD" maxLength={3} required/>
              <Feedback name="currency">
                Use a 3-letter currency code
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Button color="primary" type="submit">{book.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/books">Cancel</Button>
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

export default withRouter(BookEdit);
