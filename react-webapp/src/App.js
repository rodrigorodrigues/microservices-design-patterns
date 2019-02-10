import React, { Component } from 'react';
import './App.css';
import Home from './home/Home';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import PersonList from './person/PersonList';
import PersonEdit from './person/PersonEdit';
import Login from './login/Login';
import UserContext from './UserContext';
import UserList from "./user/UserList";
import CategoryList from "./WeekMenu/CategoryList";
import CategoryEdit from "./WeekMenu/CategoryEdit";
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
    const localStorageJwt = localStorage.getItem('JWT');
    if (localStorageJwt) {
      this.decodeJwt(localStorageJwt);
    }
  }

  setAuthentication = (data) => {
    let token = data.id_token;
    this.decodeJwt(token);
    localStorage.setItem('JWT', token);
  }

  removeAuthentication = () => {
    localStorage.removeItem('JWT');
    this.setState({ isAuthenticated: false, user: null, jwt: null });
  }

  decodeJwt(token) {
    let jwtDecoded = jwt_decode(token);
    console.log("JWT Decoded: ", jwtDecoded);
    console.log("JWT Token: ", token);
    this.setState({ isAuthenticated: true, user: jwtDecoded.name, jwt: token, authorities: jwtDecoded.authorities });
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

            <Route path='/categories' exact={true}
                   component={() => <CategoryList {...this.state}
                                                onRemoveAuthentication={this.removeAuthentication}/>} />
            <Route path='/categories/:id'
                   component={() => <CategoryEdit {...this.state} />} />
          </Switch>
        </Router>
      </UserContext.Provider>
    )
  }
}

export default App;