import React, { Component } from 'react';
import './App.css';
import Home from './Home';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import GroupList from './PersonList';
import GroupEdit from './PersonEdit';
import { CookiesProvider } from 'react-cookie';

class App extends Component {
  render() {
    return (
      <CookiesProvider>
        <Router>
          <Switch>
            <Route path='/' exact={true} component={Home}/>
            <Route path='/persons' exact={true} component={GroupList}/>
            <Route path='/persons/:id' component={GroupEdit}/>
          </Switch>
        </Router>
      </CookiesProvider>
    )
  }
}

export default App;