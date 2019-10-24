import { Component } from 'react';
import { withRouter } from 'react-router-dom';
import {getWithCredentials} from "../services/ApiService";

class Logout extends Component {
  constructor(props) {
    super(props);
    console.log("state: ", this.state);
  }

  async componentDidMount() {
    let props = this.props;
    await getWithCredentials('logout', false).then(function(e) {
      props.onRemoveAuthentication();
      props.history.push('/');
    });
  }

  render() {
    return null
  }
}
export default withRouter(Logout);