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
import IngredientList from "./WeekMenu/IngredientList";
import ProductList from "./product/ProductList";
import ProductEdit from "./product/ProductEdit";
import PostList from './posts/PostList';
import PostEdit from './posts/PostEdit';
import ModalPopup from './common/Modal';

const consulUrl = process.env.REACT_APP_CONSUL_URL;
const monitoringUrl = process.env.REACT_APP_MONITORING_URL;
const grafanaUrl = process.env.REACT_APP_GRAFANA_URL;
const jaegerUrl = process.env.REACT_APP_JAEGER_URL;
const prometheusUrl = process.env.REACT_APP_PROMETHEUS_URL;

class App extends Component {
  state = {
    isLoading: true,
    isAuthenticated: false,
    user: null,
    error: null,
    jwt: null,
    authorities: [],
    notDisplayMessage: false,
    displayError: null,
    imageUrl: null
  };

  async componentDidMount() {
    try {
      console.log("App:componentDidMount:first");
      if (this.state.isAuthenticated === false) {
          let data = await getWithCredentials('authenticatedUser', false);
          if (data.id_token) {
            window.localStorage.removeItem('redirectToPreviousPage');
            this.setAuthentication(data);
          } else {
            this.redirectToIndexPage();
            this.setState({ displayError: errorMessage(data), isLoading: false});
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
    let token = data.id_token;
    token = token.replace('bearer', 'Bearer');
    this.setState({ displayError: null });
    this.decodeJwt(token);
    window.localStorage.setItem('jhi-authenticationToken', token.slice(7, token.length));
  }

  removeAuthentication = () => {
    this.setState({ isAuthenticated: false, user: null, jwt: null });
    this.removeSessionIdCookie();
    window.localStorage.removeItem('jhi-authenticationToken');
  }

  decodeJwt(token) {
    let jwtDecoded = jwt_decode(token);
    let username = (jwtDecoded.fullName !== undefined ? jwtDecoded.fullName : jwtDecoded.sub);
    this.setState({ isAuthenticated: true, user: username, jwt: token, authorities: jwtDecoded.authorities, imageUrl: jwtDecoded.imageUrl });
  }

  render() {
    const { displayError, isAuthenticated, authorities } = this.state;
    const isAdmin = isAuthenticated && authorities.some(item => item === "ROLE_ADMIN");

    return (
      <UserContext.Provider value={this.state}>
        <Router>
          <Switch>
            <Route path='/' exact={true} 
              component={() =><Home {...this.state} error={this.state.error} />} />
            <Route path='/home' exact={true} 
              component={() =><Home {...this.state} error={this.state.error} />} />
            <Route path='/login' exact={true} 
              component={() => <Login {...this.state} 
                setAuthentication={this.setAuthentication} />} />
            <Route path='/logout' exact={true} 
              component={() => <Logout {...this.state} onRemoveAuthentication={this.removeAuthentication} />} />
            <Route path='/people' exact={true} 
              component={() => <PersonList {...this.state} />} />
            <Route path='/people/:id'
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
            <Route path='/products' exact={true}
                   component={() => <ProductList {...this.state} />} />
            <Route path='/products/:id'
                   component={() => <ProductEdit {...this.state} />} />
            <Route path='/posts' exact={true}
                   component={() => <PostList {...this.state} />} />
            <Route path='/posts/:id'
                   component={() => <PostEdit {...this.state} />} />
            <Route path='/consul' exact={true} component={() => <ModalPopup link={consulUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/monitoring' component={() => <ModalPopup link={monitoringUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/grafana' component={() => <ModalPopup link={grafanaUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/tracing' component={() => <ModalPopup link={jaegerUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/prometheus' component={() => <ModalPopup link={prometheusUrl} modal={isAdmin} {...this.state} />} />
            <Route path='/ingredients' exact={true}
                   component={() => <IngredientList {...this.state} />} />
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