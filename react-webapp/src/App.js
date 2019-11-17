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
import {getWithCredentials} from "./services/ApiService";
import MessageAlert from './MessageAlert';
import {errorMessage} from './common/Util';
import Cookies from 'js-cookie'
import TaskList from "./task/TaskList";
import TaskEdit from "./task/TaskEdit";


const eurekaUrl = process.env.REACT_APP_EUREKA_URL;
const monitoringUrl = process.env.REACT_APP_MONITORING_URL;
const grafanaUrl = process.env.REACT_APP_GRAFANA_URL;
const zipkinUrl = process.env.REACT_APP_ZIPKIN_URL;

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
    try {
      if (this.state.isAuthenticated === false) {
          let data = await getWithCredentials('authenticatedUser', false);
          console.log("User is Authenticated?", data);
          console.log("Href", window.location.href);
          if (data.id_token) {
            window.localStorage.removeItem('redirectToPreviousPage');
            this.setAuthentication(data);
          } else {
            this.redirectToIndexPage();
            this.setState({ displayError: errorMessage(data), isLoading: false});
          }
        }
    } catch (e) {
      console.log("Error when trying to connect to Session Redis", e);
      this.setState({ displayError: errorMessage(`{"error": "${e}"}`)});
      this.redirectToIndexPage();
    }
  }

  redirectToIndexPage() {
    if (!window.location.href.endsWith('/') && !window.location.href.endsWith('/login')) {
      window.localStorage.setItem('redirectToPreviousPage', window.location.href);
      if (this.props.history !== undefined) {
        this.props.history.push('/');
      } else {
        window.location.href = '/';
      }
    }
  }

  removeSessionIdCookie() {
    console.log("UseCookies: ", Cookies.get('SESSIONID'));
    Cookies.remove('SESSIONID');
  }

  setAuthentication = (data) => {
    let token = data.id_token;
    this.setState({ displayError: null });
    this.decodeJwt(token);
  }

  removeAuthentication = () => {
    this.setState({ isAuthenticated: false, user: null, jwt: null });
    this.removeSessionIdCookie();
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
            <Route path='/tasks' exact={true}
                   component={() => <TaskList {...this.state} />} />
            <Route path='/tasks/:id'
                   component={() => <TaskEdit {...this.state} />} />
            <Route path='/admin-eureka' component={() => {
              console.log("Eureka URL: ", eurekaUrl);
              window.location.href = `${eurekaUrl}`; 
              return null;
            }} />
            <Route path='/admin-monitoring' component={() => {
              console.log("Monitoring URL: ", monitoringUrl);
              window.location.href = `${monitoringUrl}`; 
              return null;
            }} />
            <Route path='/admin-grafana' component={() => {
              console.log("Grafana URL: ", grafanaUrl);
              window.location.href = `${grafanaUrl}`; 
              return null;
            }} />
            <Route path='/admin-tracing' component={() => {
              console.log("Zipkin URL: ", zipkinUrl);
              window.location.href = `${zipkinUrl}`; 
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