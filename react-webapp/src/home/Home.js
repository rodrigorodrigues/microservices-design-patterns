import React, { Component } from 'react';
import '../App.css';
import AppNavbar from './AppNavbar';
import HomeContent from './HomeContent';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import FooterContent from './FooterContent';

class Home extends Component {
  constructor(props) {
    super(props);
    this.state = {
      authorities: props.authorities,
      isAuthenticated: props.isAuthenticated,
      error: props.error,
      user: props.user
    };
  }


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
        <HomeContent {...this.state} />
        {this.displayMessage()}
        <FooterContent></FooterContent>
      </div>
    );
  }
}




export default Home;
