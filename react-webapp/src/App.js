import React, { Component } from 'react';
import './App.css';
import Home from './home/Home';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import PersonList from './person/PersonList';
import PersonEdit from './person/PersonEdit';
import Login from './login/Login';
import Logout from './login/Logout';
import UserContext from './UserContext';
import UserList from "./user/UserList";
import UserEdit from "./user/UserEdit";
import CategoryList from "./WeekMenu/CategoryList";
import CategoryEdit from "./WeekMenu/CategoryEdit";
import RecipeList from "./WeekMenu/RecipeList";
import jwt_decode from 'jwt-decode';
import {getWithoutCredentials} from "./services/ApiService";
import MessageAlert from './MessageAlert';
import {errorMessage} from './common/Util';
const eurekaUrl = process.env.REACT_APP_EUREKA_URL;
const monitoringUrl = process.env.REACT_APP_MONITORING_URL;

class App extends Component {
  state = {
    isLoading: true,
    isAuthenticated: false,
    user: null,
    error: null,
    jwt: null,
    authorities: [],
    notDisplayMessage: false,
    displayError: null
  };

  async componentDidMount() {
/*    try {
      let data = await getWithoutCredentials('sharedSessions', false);
      console.log("Shared Session", data);
    } catch (e) {
      console.log("Error when trying to connect to Session Redis", e);
      this.setState({ displayError: errorMessage(`{"error": "${e}"}`)});
    }*/
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
    const {displayError} = this.state;

    return (
      <UserContext.Provider value={this.state}>
        <Router>
          <Switch>
            <Route path='/' exact={true} 
              component={() =><Home error={this.state.error} />} />
            <Route path='/login' exact={true} 
              component={() => <Login {...this.state} 
                setAuthentication={this.setAuthentication} />} />
            <Route path='/logout' exact={true} 
              component={() => <Logout {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/persons' exact={true} 
              component={() => <PersonList {...this.state} />} />
            <Route path='/persons/:id'
              component={() => <PersonEdit {...this.state} />} />
            <Route path='/users' exact={true}
                   component={() => <UserList {...this.state} />} />
            <Route path='/users/:id'
                   component={() => <UserEdit {...this.state} />} />
            <Route path='/categories' exact={true}
                   component={() => <CategoryList {...this.state} />} />
            <Route path='/categories/:id'
                   component={() => <CategoryEdit {...this.state} />} />
            <Route path='/recipes' exact={true}
                   component={() => <RecipeList {...this.state} />} />
            <Route path='/admin-eureka' component={() => {
              window.location.href = `${eurekaUrl}`; 
              return null;
            }} />
            <Route path='/admin-monitoring' component={() => {
              window.location.href = `${monitoringUrl}`; 
              return null;
            }} />
          </Switch>
        </Router>
        <div>
          <MessageAlert {...displayError}></MessageAlert>
        </div>
      </UserContext.Provider>
    );
  }
}

export default App;