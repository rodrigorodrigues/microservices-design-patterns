import React, { Component } from 'react';
import '../App.css';
import AppNavbar from './AppNavbar';
import HomeContent from './HomeContent';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import FooterContent from './FooterContent';

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
      <div className="content">
        <AppNavbar />
        <HomeContent></HomeContent>
        {this.displayMessage()}
        <FooterContent></FooterContent>
      </div>
    );
  }
}




export default Home;
