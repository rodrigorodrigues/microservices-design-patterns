import React, {Component} from 'react';
import {Button, ButtonGroup, Container, Table} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import { EventSourcePolyfill } from 'event-source-polyfill';
import HomeContent from '../home/HomeContent';

const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;

class UserList extends Component {
  constructor(props) {
    super(props);
    this.state = {users: [], isLoading: true, jwt: props.jwt, displayError: null};
    this.remove = this.remove.bind(this);
  }

  componentDidMount() {
    let jwt = this.state.jwt;
    console.log("JWT: ", jwt);
    if (jwt) {
     //const eventSource = new EventSource(`api/users?Authorization=${this.state.jwt}`, {withCredentials: true});
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
        console.log('EventSource result: ', result);
        const data = JSON.parse(result.data);
        this.state.users.push(data);
        this.setState({users: this.state.users});
      });

      eventSource.addEventListener("error", err => {
        console.log('EventSource error: ', err);
        eventSource.close();
        this.setState({isLoading: false});
        if (err.status === 401 || err.status === 403) {
          this.props.onRemoveAuthentication();
          this.props.history.push('/');
        }
        if (this.state.users.length === 0) {
          this.setState({displayError: errorMessage(JSON.stringify(err))});
        }
      });
    }
  }

  async remove(id) {
    await fetch(`/api/users/${id}`, {
      method: 'DELETE',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      credentials: 'include'
    }).then(() => {
      let users = [...this.state.users].filter(i => i.id !== id);
      this.setState({users: users});
    });
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
            <Button size="sm" color="danger" onClick={() => this.remove(user.id)}>Delete</Button>
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