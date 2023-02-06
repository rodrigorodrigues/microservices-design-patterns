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
import {getWithCredentials, post} from "./services/ApiService";
import MessageAlert from './MessageAlert';
import {errorMessage} from './common/Util';
import Cookies from 'js-cookie'
import TaskList from "./task/TaskList";
import TaskEdit from "./task/TaskEdit";
import IngredientList from "./WeekMenu/IngredientList";
import ProductList from "./product/ProductList";
import ProductEdit from "./product/ProductEdit";
import PostList from './posts/PostList';
import PostEdit from './posts/PostEdit';
import CompanyList from "./company/CompanyList";
import CompanyEdit from "./company/CompanyEdit";
import ModalPopup from './common/Modal';
import CreateAll from "./admin/CreateAll";
import ActivityDetector from 'react-activity-detector';
import { postWithHeaders } from './services/ApiService';

const moment = require('moment');

const consulUrl = process.env.REACT_APP_CONSUL_URL;
const monitoringUrl = process.env.REACT_APP_MONITORING_URL;
const grafanaUrl = process.env.REACT_APP_GRAFANA_URL;
const jaegerUrl = process.env.REACT_APP_JAEGER_URL;
const prometheusUrl = process.env.REACT_APP_PROMETHEUS_URL;

class App extends Component {
  state = {
    isAuthenticated: window.localStorage.getItem('jhi-isAuthenticated'),
    user: window.localStorage.getItem('jhi-user'),
    error: null,
    jwt: window.localStorage.getItem('jhi-authenticationToken'),
    authorities: [],
    notDisplayMessage: false,
    displayError: null,
    imageUrl: window.localStorage.getItem('jhi-imageUrl'),
    refreshToken: window.localStorage.getItem('jhi-refreshToken'),
    expiresIn: window.localStorage.getItem('jhi-expiresIn')
  };

  async componentDidMount() {
    try {
      if (window.location.href.endsWith('/login')) {
        return;
      }
      if (this.state.authorities.length === 0 && window.localStorage.getItem('jhi-authorities')) {
        this.setState({authorities: JSON.parse(window.localStorage.getItem('jhi-authorities'))});
      }
      if (!this.state.isAuthenticated) {
          let data = await getWithCredentials('authenticatedUser', false);
          if (data.access_token) {
            window.localStorage.removeItem('redirectToPreviousPage');
            this.setAuthentication(data);
          } else {
            if (data.status === 401 && this.state.refreshToken && moment().isAfter(this.state.expiresIn)) {
              console.log(`Token is expired trying to refresh_token:`);
              const body = "refresh_token=" + encodeURIComponent(this.state.refreshToken);
              data = await post('refreshToken', body);
              if (data.access_token) {
                window.localStorage.removeItem('redirectToPreviousPage');
                this.setAuthentication(data);
                return;
              }
            }
            this.redirectToIndexPage();
            this.setState({ displayError: errorMessage(data)});
            this.removeAuthentication();
          }
        }
    } catch (e) {
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
    Cookies.remove('SESSIONID');
  }

  setAuthentication = (data) => {
    let token = data.access_token;
    this.setState({ displayError: null });
    this.decodeJwt(`Bearer ${token}`, data.refresh_token, data.expires_in);
    window.localStorage.setItem('jhi-isAuthenticated', true);
  }

  removeAuthentication = () => {
    this.setState({ isAuthenticated: false, user: null, jwt: null });
    this.removeSessionIdCookie();
    window.localStorage.removeItem('jhi-authenticationToken');
    window.localStorage.removeItem('jhi-isAuthenticated');
    window.localStorage.removeItem('jhi-refreshToken');
    window.localStorage.removeItem('jhi-expiresIn');
    window.localStorage.removeItem('jhi-authorities');
    window.localStorage.removeItem('jhi-user');
    window.localStorage.removeItem('jhi-imageUrl');
  }

  decodeJwt(token, refresh_token, expires_in) {
    const jwtDecoded = jwt_decode(token);
    const user = (jwtDecoded.fullName !== undefined ? jwtDecoded.fullName : jwtDecoded.sub);
    const expiresIn = moment().add(expires_in, 'seconds').toDate();
    this.setState({ 
      isAuthenticated: true, 
      user: user, 
      jwt: token, 
      authorities: jwtDecoded.authorities, 
      imageUrl: jwtDecoded.imageUrl,
      refreshToken: refresh_token,
      expiresIn: expiresIn
    });
    window.localStorage.setItem('jhi-user', user);
    window.localStorage.setItem('jhi-imageUrl', jwtDecoded.imageUrl);
    window.localStorage.setItem('jhi-refreshToken', refresh_token);
    window.localStorage.setItem('jhi-expiresIn', expiresIn);
    window.localStorage.setItem('jhi-authorities', JSON.stringify(jwtDecoded.authorities));
    window.localStorage.setItem('jhi-authenticationToken', token);
  }

  onIdle = () => {
    console.log("The user seems to be idle... " + new Date() + " - redirect to logout");
    window.location.href = '/logout';
  }
    
  onActive = async () => {
    const { expiresIn, refreshToken, jwt } = this.state;
    if (expiresIn != null && refreshToken != null) {
      const expIn = moment(expiresIn).subtract(1, 'minute');
      if (moment().isSameOrAfter(expIn)) {
        console.log("Refreshing Token");
        await this.refreshTokenCall(refreshToken, jwt);
      }
    }
  }

  async refreshTokenCall(refreshToken, jwt) {
    const loginSubmit = "refresh_token=" + encodeURIComponent(refreshToken);
    try {
      const data = await postWithHeaders('authenticate', loginSubmit, { 
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 
        'Authorization': jwt
      });
      if (data.access_token) {
        this.setAuthentication(data);
      } else {
        this.setState({ displayError: errorMessage(data) });
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    }
  }

  render() {
    const { displayError, isAuthenticated, authorities } = this.state;
    const isAdmin = isAuthenticated && authorities.some(item => item === "ROLE_ADMIN");

    return (
      <UserContext.Provider value={this.state}>
        <Router>
          <Switch>
            <Route path='/' exact={true} 
              component={() =><Home {...this.state} error={this.state.error} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/home' exact={true} 
              component={() =><Home {...this.state} error={this.state.error} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/login' exact={true} 
              component={() => <Login {...this.state} 
                setAuthentication={this.setAuthentication} />} />
            <Route path='/logout' exact={true} 
              component={() => <Logout {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/people' exact={true} 
              component={() => <PersonList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/people/:id'
              component={() => <PersonEdit {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/users' exact={true}
                   component={() => <UserList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/users/:id'
                   component={() => <UserEdit {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/categories' exact={true}
                   component={() => <CategoryList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/categories/:id'
                   component={() => <CategoryEdit {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/recipes' exact={true}
                   component={() => <RecipeList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/tasks' exact={true}
                   component={() => <TaskList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/tasks/:id'
                   component={() => <TaskEdit {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/companies' exact={true}
                   component={() => <CompanyList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/companies/:id'
                   component={() => <CompanyEdit {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/products' exact={true}
                   component={() => <ProductList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/products/:id'
                   component={() => <ProductEdit {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/posts' exact={true}
                   component={() => <PostList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/posts/:id'
                   component={() => <PostEdit {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/consul' exact={true} component={() => <ModalPopup link={consulUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/monitoring' component={() => <ModalPopup link={monitoringUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/grafana' component={() => <ModalPopup link={grafanaUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/tracing' component={() => <ModalPopup link={jaegerUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/prometheus' component={() => <ModalPopup link={prometheusUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/ingredients' exact={true}
                   component={() => <IngredientList {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/admin/createAll'
                   component={() => <CreateAll {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
          </Switch>
        </Router>
        <div>
          <MessageAlert {...displayError}></MessageAlert>
        </div>
        <ActivityDetector enabled={true} timeout={60 * 1000 * 30} onIdle={this.onIdle} onActive={this.onActive}/>
      </UserContext.Provider>
    );
  }
}

export default App;