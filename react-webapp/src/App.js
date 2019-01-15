import React, { Component } from 'react';
import './App.css';
import Home from './home/Home';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import PersonList from './person/PersonList';
import PersonEdit from './person/PersonEdit';
import Login from './login/Login';
import UserContext from './UserContext';
import UserList from "./user/UserList";
import jwt_decode from 'jwt-decode';

class App extends Component {
  state = {
    isLoading: true,
    isAuthenticated: false,
    user: null,
    error: null,
    jwt: null,
    authorities: []
  };

  async componentDidMount() {
    const localStateParent = localStorage.getItem('stateParent');
    if (localStateParent) {
      this.state = JSON.parse(localStateParent);
      this.setState({ isAuthenticated: this.state.isAuthenticated, user: this.state.user, jwt: this.state.jwt });
    }
  }

  setAuthentication = (data) => {
    let jwtDecoded = jwt_decode(data.id_token);
    console.log("JWT Decoded: ", jwtDecoded);
    this.setState({ isAuthenticated: true, user: jwtDecoded.name, jwt: data.id_token, authorities: jwtDecoded.authorities });
    localStorage.setItem('stateParent', JSON.stringify(this.state));
  }

  removeAuthentication = () => {
    localStorage.removeItem('stateParent');
    this.setState({ isAuthenticated: false, user: null, jwt: null });
  }
  render() {
    return (
      <UserContext.Provider value={this.state}>
        <Router>
          <Switch>
            <Route path='/' exact={true} 
              component={() =><Home error={this.state.error} 
              onRemoveAuthentication={this.removeAuthentication}/>} />
            <Route path='/login' exact={true} 
              component={() => <Login {...this.state} 
                setAuthentication={this.setAuthentication} />} />
            <Route path='/persons' exact={true} 
              component={() => <PersonList {...this.state}
              onRemoveAuthentication={this.removeAuthentication}/>} />
            <Route path='/persons/:id'
              component={() => <PersonEdit {...this.state} />} />
            <Route path='/users' exact={true}
                   component={() => <UserList {...this.state}
                                                onRemoveAuthentication={this.removeAuthentication}/>} />
            <Route path='/users/:id'
                   component={() => <PersonEdit {...this.state} />} />
          </Switch>
        </Router>
      </UserContext.Provider>
    )
  }
}

export default App;