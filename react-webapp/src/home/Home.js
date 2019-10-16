import React, { Component } from 'react';
import '../App.css';
import AppNavbar from './AppNavbar';
import HomeContent from './HomeContent';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';

class Home extends Component {

  displayMessage = () => {
    const { error } = this.props;
    if(error) {
      return <MessageAlert {... errorMessage(error)}></MessageAlert>
    }
    return ''
  }

  render() {
    return (
      <div>
        <AppNavbar />
        <HomeContent></HomeContent>
        {this.displayMessage()}
      </div>
    );
  }
}




export default Home;
