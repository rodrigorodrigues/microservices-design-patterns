import { Component } from 'react';
import { withRouter } from 'react-router-dom';

class Logout extends Component {
  async componentDidMount() {
    this.props.onRemoveAuthentication();
    this.props.history.push('/');
  }

  render() {
    return null
  }
}
export default withRouter(Logout);