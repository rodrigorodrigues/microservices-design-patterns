import React, { Component } from 'react';
import '../App.css';
import AppNavbar from './AppNavbar';
import HomeContent from './HomeContent';
import MessageAlert from '../MessageAlert';
import { errorMessage, marginLeft } from '../common/Util';
import FooterContent from './FooterContent';
import { Container, Table } from 'reactstrap';
import { toast } from 'react-toastify';
import Card from "./Card";
import Menu from "./Menu";
import withRouter from '../common/WithRouter';

const styles = {
  fontFamily: "sans-serif",
  textAlign: "center"
};

class Home extends Component {
  constructor(props) {
    super(props);
    this.state = {
      authorities: props.authorities,
      isAuthenticated: props.isAuthenticated,
      error: props.error,
      user: props.user,
      expanded: false,
      jwt: props.jwt,
      imageUrl: props.imageUrl
    };
  }

  componentDidMount() {
    toast.dismiss('Error');
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
    <h2>Welcome, {user}!</h2> : '';
  }

  displayHomeScrollView = () => {
    return (<div style={styles}>
      <Menu cards={4} />
      <Card card="1" />
      <Card card="2" bgcolor="#eee" />
      <Card card="3" bgcolor="#ccc" />
      <Card card="4" bgcolor="#ddd" />
    </div>);
  }

  displayImageUrl = () => {
    const { imageUrl } = this.props;
    if (imageUrl) {
      return (<tr>
        <td>&nbsp;</td>
        <td><img src={imageUrl} alt="avatar" /></td>
      </tr>)
    } else {
      return null;
    }
  }

  displayUserPermissions = () => {
        const { user, jwt, authorities } = this.props;
        if (!user) {
            return null;
        }
        return (<div className="jumbotron" fluid>
                <h1 className="display-5">User Details Permission</h1>
                <hr className="my-2" />
                <Table size="sm" borderless>
                    <tbody>
                        <tr>
                            <td><b>User</b></td>
                            <td>{user}</td>
                        </tr>
                        {this.displayImageUrl()}
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
        </div>
        )
  }

  render() {
    const { expanded, isAuthenticated } = this.state;

    return (
      <div
      style={{
          marginLeft: (isAuthenticated ? marginLeft(expanded) : 0),
          padding: (isAuthenticated ? '15px 20px 0 20px' : '')
      }}
      >
        <AppNavbar/>
        <Container fluid>
        {isAuthenticated && <HomeContent setExpanded={this.setExpanded} {...this.state} />}
        {!isAuthenticated && this.displayHomeScrollView()}
        {this.displayMessage()}
        {this.displayUserPermissions()}
        <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(Home);
