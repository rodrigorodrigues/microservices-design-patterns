import React, {Component} from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import { EventSourcePolyfill } from 'event-source-polyfill';
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import Iframe from 'react-iframe';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';

const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;
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
      authorities: props.authorities
    };
    this.remove = this.remove.bind(this);
    this.toggle = this.toggle.bind(this);
  }

  toggle(tab) {
    if (this.state.activeTab !== tab) {
      this.setState({ activeTab: tab });
    }
  }

  componentDidMount() {
    toast.dismiss('Error');
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        const eventSource = new EventSourcePolyfill(`${gatewayUrl}/api/users`, {
          headers: {
            'Authorization': jwt
          }
        });

        eventSource.addEventListener("open", result => {
          console.log('EventSource open: ', result);
          this.setState({isLoading: false, displaySwagger: true});
        });

        eventSource.addEventListener("message", result => {
          const data = JSON.parse(result.data);
          this.state.users.push(data);
          this.setState({users: this.state.users});
        });

        eventSource.addEventListener("error", err => {
          console.log('EventSource error: ', err);
          eventSource.close();
          this.setState({isLoading: false});
          if (err.status === 401) {
            this.setState({displayAlert: true});
          } else {
            if (this.state.users.length === 0) {
              this.setState({displayError: errorMessage(JSON.stringify(err))});
            }
          }
        });
      }
    }
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
    const { users, isLoading, displayError, displayAlert, displaySwagger} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const userList = users.map(user => {
      return <tr key={user.id}>
        <th scope="row">{user.id}</th>
        <td style={{whiteSpace: 'nowrap'}}>{user.email}</td>
        <td>{user.fullName}</td>
        <th>{user.createdByUser}</th>
        <td>{user.createdDate}</td>
        <td>{user.lastModifiedByUser}</td>
        <td>{user.lastModifiedDate}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/users/" + user.id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': user.id, 'name': user.fullName})}>Delete</Button>
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
            <div className="float-right">
              <Button color="success" tag={Link} to="/users/new" disabled={displayAlert}>Add User</Button>
            </div>
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
                <th>Actions</th>
              </tr>
              </thead>
              <tbody>
              {userList}
              </tbody>
            </Table>
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
      <div>
        <AppNavbar/>
        <Container fluid>
          <HomeContent notDisplayMessage={true}></HomeContent>
          {displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(UserList);