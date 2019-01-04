import React, { Component } from 'react';
import './App.css';
import Home from './home/Home';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import PersonList from './person/PersonList';
import PersonEdit from './person/PersonEdit';
import Login from './login/Login';
import { get } from './services/ApiService';
class App extends Component {
  state = {
    isLoading: true,
    isAuthenticated: false,
    user: null,
    jwt: null,
    error: null
  };

  async componentDidMount() {
    const localStateParent = localStorage.getItem('stateParent');
    if (localStateParent) {
      this.state = JSON.parse(localStateParent);
      this.setState({ isAuthenticated: this.state.isAuthenticated, user: this.state.user, jwt: this.state.jwt });
    } else {
      try {
        const body = await get('authenticate', true);
        if (body === '') {
          this.setState(({ isAuthenticated: false }))
        } else {
          this.setState({ isAuthenticated: true, user: JSON.parse(body) })
        }
      } catch (error) {
        this.setState({error: error.message})
      }
    }
  }

  setAuthentication = (data) => {
    this.setState({ isAuthenticated: true, user: data.user, jwt: data.id_token });
    localStorage.setItem('stateParent', JSON.stringify(this.state));
  }

  removeAuthentication = () => {
    localStorage.removeItem('stateParent');
    this.setState({ isAuthenticated: false, user: null, jwt: null });
  }

  render() {
    return (
      <Router>
        <Switch>
          <Route path='/' exact={true} 
            component={() => <Home {...this.state} 
              removeAuthentication={this.removeAuthentication} />} />
          <Route path='/login' exact={true} 
            component={() => <Login {...this.state} 
              setAuthentication={this.setAuthentication} />} />
          <Route path='/persons' exact={true} 
            component={() => <PersonList {...this.state} />} />
          <Route path='/persons/:id' 
            component={() => <PersonEdit {...this.state} />} />
        </Switch>
      </Router>
    )
  }
}

export default App;