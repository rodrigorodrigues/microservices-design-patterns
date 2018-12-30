import React, {Component} from 'react';
import './App.css';
import AppNavbar from './AppNavbar';
import {Link} from 'react-router-dom';
import {Button, Container} from 'reactstrap';

class Home extends Component {
  constructor(props) {
    super(props);
    this.logout = this.logout.bind(this);
  }

  logout() {
    fetch('/logout', {method: 'GET'})
        .then(() => {
            this.props.removeAuthentication();
        });
  }

  render() {
    const message = this.props.stateParent.user ?
      <h2>Welcome, {this.props.stateParent.user.name}!</h2> :
      <p>Please log in to manage your JUG Tour.</p>;

    const button = this.props.stateParent.isAuthenticated ?
      <div>
        <Button color="link"><Link to="/persons">Manage JUG Tour</Link></Button>
        <br/>
        <Button color="link" onClick={this.logout}>Logout</Button>
      </div> :
      <Button color="link"><Link to="/login">Login</Link></Button>;

    return (
      <div>
        <AppNavbar/>
        <Container fluid>
          {message}
          {button}
        </Container>
      </div>
    );
  }
}

export default Home;