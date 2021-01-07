import React, {Component} from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import { errorMessage, marginLeft } from '../common/Util';
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import Iframe from 'react-iframe';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';
import { get } from "../services/ApiService";
import PaginationComponent from "../common/Pagination";
import SearchButtonComponent from "../common/Search";

const userSwaggerUrl = process.env.REACT_APP_USER_SWAGGER_URL;

class UserList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      users: [], 
      isLoading: true, 
      jwt: props.jwt, 
      displayError: null, 
      displayAlert: false,
      displaySwagger: false,
      activeTab: '1',
      authorities: props.authorities,
      isAuthenticated: props.isAuthenticated,
      user: props.user,
      expanded: false,
      activePage: 0,
      totalPages: null,
      itemsCountPerPage: null,
      totalItemsCount: null,
      pageSize: 10
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
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

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    this.setState({search: value});
  }

  async handleSubmit(event) {
    event.preventDefault();
    await this.findAllUsers();
  }

  async componentDidMount() {
    toast.dismiss('Error');
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        await this.findAllUsers();
      }
    }
  }

  async findAllUsers(pageNumber) {
    try {
      const { pageSize, activePage, search, jwt } = this.state;
      let url = `users?${search ? search : ''}${pageNumber !== undefined ? '&page='+pageNumber : activePage ? '&page='+activePage : ''}${pageSize ? '&pageSize='+pageSize: ''}`;
      console.log("URL: {}", url);
      let data = await get(url, true, false, jwt);
      if (data) {
        if (Array.isArray(data.content)) {
          this.setState({ 
            isLoading: false, 
            users: data.content, 
            displaySwagger: true, 
            totalPages: data.totalPages, 
            itemsCountPerPage: data.size, 
            totalItemsCount: data.totalElements
          });
          this.props.history.push(`/${url}`);
        } else {
          if (data.status === 401) {
            this.setState({ displayAlert: true });
          }
          this.setState({ isLoading: false, displayError: errorMessage(data) });
        }
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    }
  }

  async handlePageChange(pageNumber) {
    console.log(`active page is ${pageNumber}`);
    this.setState({activePage: pageNumber})
    await this.findAllUsers(pageNumber);
  }

  async remove(user) {
    let confirm = await confirmDialog(`Delete User ${user.name}`, "Are you sure you want to delete this?", "Delete User");
    if (confirm) {
      let id = user.id;
      let jwt = this.state.jwt;
      await fetch(`/api/users/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt
        },
        credentials: 'include'
      }).then((err) => {
        if (err.status !== 200) {
          this.setState({displayError: errorMessage(err)});
        } else {
          let users = [...this.state.users].filter(i => i.id !== id);
          this.setState({users: users});
        }
      });
    }
  }

  render() {
    const { users, isLoading, displayError, displayAlert, displaySwagger, expanded} = this.state;

    const userList = users.map(user => {
      return <tr key={user.id}>
        <th scope="row">{user.id}</th>
        <td style={{whiteSpace: 'nowrap'}}>{user.email}</td>
        <td>{user.fullName}</td>
        <th>{user.createdByUser}</th>
        <td>{user.createdDate}</td>
        <td>{user.lastModifiedByUser}</td>
        <td>{user.lastModifiedDate}</td>
        <td>{user.userType === 'GOOGLE' ? <i class="fa fa-google" title="Connected by Google" aria-hidden="true"></i> : ''}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/users/" + user.id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': user.id, 'name': user.fullName})}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    const displayContent = () => {
      const suggestions = [
        {
          title: 'name',
          languages: [
            {
              name: 'C',
              year: 1972
            }
          ]
        }
      ];

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
              Users
            </NavLink>
          </NavItem>
          <NavItem>
            <NavLink
              className={classnames({ active: this.state.activeTab === '2' })}
              onClick={() => { this.toggle('2'); }}>
              Swagger UI
            </NavLink>
          </NavItem>
        </Nav>
        <TabContent activeTab={this.state.activeTab}>
          <TabPane tabId="1">
            <div className="float-right"
                  style={{
                    padding: '15px 20px 0 20px'
                  }}
            >
              <Button color="success" tag={Link} to="/users/new" disabled={displayAlert}>Add User</Button>
            </div>
            <SearchButtonComponent handleChange={this.handleChange} handleSubmit={this.handleSubmit} suggestions={suggestions} />
            <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
            <Table striped responsive>
              <thead>
              <tr>
                <th>#</th>
                <th>Email</th>
                <th>Name</th>
                <th>Created By</th>
                <th>Created Date</th>
                <th>Last Modified By User</th>
                <th>Last Modified By Date</th>
                <th>User Type</th>
                <th>Actions</th>
              </tr>
              </thead>
              <tbody>
              {userList}
              </tbody>
            </Table>
            <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
          </TabPane>
          <TabPane tabId="2">
            {displaySwagger ?
              <Iframe url={`${userSwaggerUrl}`}
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
        <AppNavbar/>
        <Container fluid>
          <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
          {isLoading && <i class="fa fa-spinner" aria-hidden="true"></i>}
          {!isLoading && displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(UserList);