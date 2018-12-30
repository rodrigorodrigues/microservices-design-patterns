import React, { Component } from 'react';
import './App.css';
import Home from './Home';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import PersonList from './PersonList';
import PersonEdit from './PersonEdit';
import Login from './Login';

class App extends Component {
  state = {
    isLoading: true,
    isAuthenticated: false,
    user: null,
    jwt: null
  };

  constructor(props) {
    super(props);
    this.setAuthentication = this.setAuthentication.bind(this);
    this.removeAuthentication = this.removeAuthentication.bind(this);
  }

  async componentDidMount() {
    const localStateParent = localStorage.getItem('stateParent');
    if (localStateParent) {
      this.state = JSON.parse(localStateParent);
      this.setState({isAuthenticated: this.state.isAuthenticated, user: this.state.user, jwt: this.state.jwt});
    } else {
      const response = await fetch('/api/authenticate', {credentials: 'include'});
      const body = await response.text();
      if (body === '') {
        this.setState(({isAuthenticated: false}))
      } else {
        this.setState({isAuthenticated: true, user: JSON.parse(body)})
      }
    }
  }

  setAuthentication(data) {
    this.setState({isAuthenticated: true, user: data.user, jwt: data.id_token});
    localStorage.setItem('stateParent', JSON.stringify(this.state));
  }

  removeAuthentication() {
    localStorage.removeItem('stateParent');
    this.setState({isAuthenticated: false, user: null, jwt: null});
  }

  render() {
    return (
      <Router>
        <Switch>
          <Route path='/' exact={true} component={() => <Home stateParent={this.state} removeAuthentication={this.removeAuthentication} />}/>
          <Route path='/login' exact={true} stateParent={this.state} component={() => <Login setAuthentication={this.setAuthentication} stateParent={this.state} />}/>
          <Route path='/persons' exact={true} stateParent={this.state} component={() => <PersonList stateParent={this.state} />}/>
          <Route path='/persons/:id' stateParent={this.state} component={() => <PersonEdit stateParent={this.state} />}/>
        </Switch>
      </Router>
    )
  }
}

export default App;