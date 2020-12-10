import React, { Component } from 'react';
import '../App.css';
import AppNavbar from './AppNavbar';
import HomeContent from './HomeContent';
import MessageAlert from '../MessageAlert';
import { errorMessage, marginLeft } from '../common/Util';
import FooterContent from './FooterContent';
import { Container, Jumbotron, Table } from 'reactstrap';
import { withRouter } from 'react-router-dom';
import { toast } from 'react-toastify';

class Home extends Component {
  constructor(props) {
    super(props);
    this.state = {
      authorities: props.authorities,
      isAuthenticated: props.isAuthenticated,
      error: props.error,
      user: props.user,
      expanded: false,
      jwt: props.jwt
    };
  }

  componentDidMount() {
    toast.dismiss('Error');
    let jwt = this.state.jwt;
    console.log("Home:componentDidMount:jwt: "+jwt);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  displayMessage = () => {
    const { error, user } = this.props;
    if(error) {
      return <MessageAlert {... errorMessage(error)}></MessageAlert>
    }
    return user ?
    <h2>Welcome, {user}!</h2> :
    <p>Please log in.</p>;
  }

  displayUserPermissions = () => {
        const { user, jwt, authorities } = this.props;
        if (!user) {
            return null;
        }
        return (<div>
            <Jumbotron fluid>
                <h1 className="display-5">User Details Permission</h1>
                <hr className="my-2" />
                <Table size="sm" borderless>
                    <tbody>
                        <tr>
                            <td><b>User</b></td>
                            <td>{user}</td>
                        </tr>
                        <tr>
                            <td><b>Authorities</b></td>
                            <td>{authorities}</td>
                        </tr>
                        <tr>
                            <td><b>JWT</b></td>
                            <td style={{ wordBreak: 'break-word' }}>{jwt}</td>
                        </tr>
                    </tbody>
                </Table>
            </Jumbotron>
        </div>
        )
  }

  render() {
    const { expanded } = this.state;

    return (
      <div
      style={{
          marginLeft: marginLeft(expanded),
          padding: '15px 20px 0 20px'
      }}
      >
        <AppNavbar/>
        <Container fluid>
        <HomeContent setExpanded={this.setExpanded} {...this.state} />
        {this.displayMessage()}
        {this.displayUserPermissions()}
        <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(Home);
