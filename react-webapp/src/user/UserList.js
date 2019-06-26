import React, {Component} from 'react';
import {Button, ButtonGroup, Container, Table} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import { EventSourcePolyfill } from 'event-source-polyfill';
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';

const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;

class UserList extends Component {
  constructor(props) {
    super(props);
    this.state = {users: [], isLoading: true, jwt: props.jwt, displayError: null};
    this.remove = this.remove.bind(this);
    this.logout = this.logout.bind(this);
  }

  componentDidMount() {
    let jwt = this.state.jwt;
    if (jwt) {
      const eventSource = new EventSourcePolyfill(`${gatewayUrl}/api/users`, {
        headers: {
          'Authorization': jwt
        }
      });

      eventSource.addEventListener("open", result => {
        console.log('EventSource open: ', result);
        this.setState({isLoading: false});
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
        if (this.state.users.length === 0) {
          this.setState({displayError: errorMessage(JSON.stringify(err))});
        }
      });
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

  async logout() {
    try {
      this.props.onRemoveAuthentication();
    } catch (error) {
      console.error(error);
    }
  }

  render() {
    const {users, isLoading, displayError} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const userList = users.map(user => {
      return <tr key={user.id}>
        <td style={{whiteSpace: 'nowrap'}}>{user.email}</td>
        <td>{user.fullName}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/users/" + user.id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': user.id, 'name': user.fullName})}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    return (
      <div>
        <AppNavbar/>
        <Container fluid>
          <div className="float-right">
            <Button color="success" tag={Link} to="/users/new">Add User</Button>
          </div>
          <div className="float-left">
            <HomeContent notDisplayMessage={true} logout={this.logout}></HomeContent>
          </div>
          <div className="float-right">
          <h3>Users</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="30%">Email</th>
              <th width="62%">Full Name</th>
            </tr>
            </thead>
            <tbody>
            {userList}
            </tbody>
          </Table>
          <MessageAlert {...displayError}></MessageAlert>
          </div>
        </Container>
      </div>
    );
  }
}

export default withRouter(UserList);