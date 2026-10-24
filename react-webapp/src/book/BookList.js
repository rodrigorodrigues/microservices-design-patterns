import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import { getResponse } from "../services/ApiService";
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import Iframe from 'react-iframe';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';
import { marginLeft } from '../common/Util';
import PaginationComponent from "../common/Pagination";
import { loading } from '../common/Loading';
import uuid from 'react-uuid';

const queryString = require('query-string');

const bookSwaggerUrl = process.env.REACT_APP_BOOK_SWAGGER_URL;

export const BOOK_READ_AUTHORITIES = ['ROLE_ADMIN', 'ROLE_BOOKS_READ', 'ROLE_BOOKS_CREATE', 'ROLE_BOOKS_SAVE', 'ROLE_BOOKS_DELETE', 'SCOPE_openid'];

class BookList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      books: [],
      isLoading: true,
      jwt: props.jwt,
      displayError: null,
      activeTab: '1',
      authorities: props.authorities,
      displayAlert: false,
      displaySwagger: false,
      expanded: false,
      isAuthenticated: props.isAuthenticated,
      // The pager is 1-based; php-service pages start at 0.
      activePage: 1,
      totalPages: null,
      itemsCountPerPage: null,
      totalItemsCount: null,
      pageSize: 10,
      gatewayUrl: props.gatewayUrl
    };
    this.handlePageChange = this.handlePageChange.bind(this);
    this.remove = this.remove.bind(this);
    this.toggle = this.toggle.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  setPageSize = (pageSize) => {
    this.setState({pageSize: pageSize});
  }

  toggle(tab) {
    if (this.state.activeTab !== tab) {
      this.setState({ activeTab: tab });
    }
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      toast.dismiss('Error');
      let jwt = this.state.jwt;
      let permissions = this.state.authorities;
      if (jwt && permissions) {
        let activePage = this.state.activePage;
        if (window.location.search !== "") {
          const parsed = queryString.parse(window.location.search);
          if (parsed.page !== undefined && Number(parsed.page) > 0) {
            activePage = Number(parsed.page);
            this.setState({activePage: activePage});
          }
        }

        if (!permissions.some(item => BOOK_READ_AUTHORITIES.includes(item))) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          await this.findAllBooks(activePage);
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  async findAllBooks(pageNumber) {
    try {
      const { pageSize, jwt } = this.state;
      const url = `books?page=${pageNumber - 1}${pageSize ? '&size=' + pageSize : ''}`;
      const response = await getResponse(url, true, false, jwt, true);
      const data = await response.json();
      if (response.status === 200 && Array.isArray(data.content)) {
        this.setState({
          isLoading: false,
          books: data.content,
          displaySwagger: true,
          totalPages: data.totalPages,
          itemsCountPerPage: data.size,
          totalItemsCount: data.totalElements
        });
        this.props.history.push(`/books?page=${pageNumber}`);
      } else {
        if (response.status === 401) {
          this.setState({ displayAlert: true });
        }
        this.setState({ isLoading: false, displayError: errorMessage(data) });
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    }
  }

  async handlePageChange(pageNumber) {
    try {
      this.setLoading(true);
      this.setState({activePage: pageNumber});
      await this.findAllBooks(pageNumber);
    } finally {
      this.setLoading(false);
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  async remove(book) {
    let confirm = await confirmDialog(`Delete Book ${book.title}`, "Are you sure you want to delete this?", "Delete Book");
    if (confirm) {
      let id = book.id;
      const { jwt, gatewayUrl } = this.state;
      await fetch(`${gatewayUrl}/api/books/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt,
          'requestId': uuid()
        },
        credentials: 'include'
      }).then(async (response) => {
        if (response.status !== 200) {
          this.setState({ displayError: errorMessage(await response.text()) });
        } else {
          let books = [...this.state.books].filter(i => i.id !== id);
          this.setState({ books: books });
        }
      });
    }
  }

  render() {
    const { books, isLoading, displayError, authorities, displayAlert, displaySwagger, expanded } = this.state;

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_BOOKS_CREATE' || item === 'SCOPE_openid');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_BOOKS_SAVE' || item === 'SCOPE_openid');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_BOOKS_DELETE' || item === 'SCOPE_openid');

    const bookList = books.map(book => {
      return <tr key={book.id}>
        <th scope="row">{book.id}</th>
        <td style={{ whiteSpace: 'nowrap' }}>{book.title}</td>
        <td style={{ whiteSpace: 'nowrap' }}>{book.author}</td>
        <td style={{ whiteSpace: 'nowrap' }}>{book.isbn}</td>
        <td>{book.publishedYear}</td>
        <td style={{ whiteSpace: 'nowrap' }}>{book.price} {book.currency}</td>
        <td>{book.createdByUser}</td>
        <td>{book.createdDate}</td>
        <td>{book.lastModifiedByUser}</td>
        <td>{book.lastModifiedDate}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/books/" + book.id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({ 'id': book.id, 'title': book.title })} disabled={!hasDeleteAccess}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
          <Nav tabs>
        <NavItem>
          <NavLink
            className={classnames({ active: this.state.activeTab === '1' })}
            onClick={() => { this.toggle('1'); }}>
            Books
        </NavLink>
        </NavItem>
        {bookSwaggerUrl &&
        <NavItem>
          <NavLink
            className={classnames({ active: this.state.activeTab === '2' })}
            onClick={() => { this.toggle('2'); }}>
            Swagger UI
        </NavLink>
        </NavItem>}
      </Nav>
      <TabContent activeTab={this.state.activeTab}>
        <TabPane tabId="1">
          <div className="float-right"
                style={{
                  padding: '15px 20px 0 20px'
                }}
          >
            <Button color="success" tag={Link} to="/books/new" disabled={!hasCreateAccess || displayAlert}>Add Book</Button>
          </div>
          <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
          <Table striped responsive>
            <thead>
              <tr>
                <th>#</th>
                <th>Title</th>
                <th>Author</th>
                <th>ISBN</th>
                <th>Published Year</th>
                <th>Price</th>
                <th>Created By</th>
                <th>Created Date</th>
                <th>Last Modified By User</th>
                <th>Last Modified By Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {bookList}
            </tbody>
          </Table>
          <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
        </TabPane>
        <TabPane tabId="2">
          {displaySwagger && bookSwaggerUrl ?
          <Iframe url={`${bookSwaggerUrl}`}
            position="absolute"
            width="100%"
            id="myId"
            className="mt-4"
            height="100%" />
            : null}
        </TabPane>
      </TabContent>
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
        <AppNavbar />
        <Container fluid>
          <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
          {loading(isLoading)}
          {!isLoading && displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(BookList);
