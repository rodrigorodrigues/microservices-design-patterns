import React, { Component } from 'react';
import './App.css';
import Home from './Home';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import PersonList from './PersonList';
import PersonEdit from './PersonEdit';
import { CookiesProvider } from 'react-cookie';

class App extends Component {
  render() {
    return (
      <CookiesProvider>
        <Router>
          <Switch>
            <Route path='/' exact={true} component={Home}/>
            <Route path='/persons' exact={true} component={PersonList}/>
            <Route path='/persons/:id' component={PersonEdit}/>
          </Switch>
        </Router>
      </CookiesProvider>
    )
  }
}

export default App;