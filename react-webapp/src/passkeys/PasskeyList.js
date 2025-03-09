import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import { get } from "../services/ApiService";
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';
import { marginLeft } from '../common/Util';
import { loading } from '../common/Loading';
import uuid from 'react-uuid';

class PasskeyList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      passkeys: [],
      isLoading: true,
      jwt: props.jwt,
      displayError: null,
      activeTab: '1',
      displayAlert: false,
      expanded: false,
      isAuthenticated: props.isAuthenticated,
      activePage: 0,
      gatewayUrl: props.gatewayUrl
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.remove = this.remove.bind(this);
    this.toggle = this.toggle.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  toggle(tab) {
    if (this.state.activeTab !== tab) {
      this.setState({ activeTab: tab });
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    this.setState({search: value});
  }

  async handleSubmit(event) {
    try {
      this.setLoading(true);
      await this.findAllPasskeys();
    } finally {
      this.setLoading(false);
    }
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      toast.dismiss('Error');
      const isAuthenticated = this.state.isAuthenticated;
      if (isAuthenticated) {
        await this.findAllPasskeys();
      } else {
        this.setState({ displayAlert: true });
        this.props.onRemoveAuthentication();
      }
    } finally {
      this.setLoading(false);
    }
  }

  async findAllPasskeys() {
    try {
      const { jwt } = this.state;
      let data = await get('webauthns', true, false, jwt);
      if (data) {
        if (Array.isArray(data)) {
          this.setState({ 
            isLoading: false, 
            passkeys: data
          });
          this.props.history.push(`/passkeys`);
        } else {
          if (data.status === 401) {
            this.setState({ displayAlert: true });
          }
          this.setState({ isLoading: false, displayError: errorMessage(data) });
        }
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  async remove(passkey) {
    let confirm = await confirmDialog(`Delete Passkey ${passkey.name}`, "Are you sure you want to delete this?", "Delete Passkey");
    if (confirm) {
      let id = passkey.id;
      const { jwt, gatewayUrl } = this.state.jwt;
      await fetch(`${gatewayUrl}/api/webauthns/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt,
          'requestId': uuid()
        },
        credentials: 'include'
      }).then((err) => {
        if (err.status !== 200) {
          this.setState({ displayError: errorMessage(err) });
        } else {
          let passkeys = [...this.state.passkeys].filter(i => i.id !== id);
          this.setState({ passkeys: passkeys });
        }
      });
    }
  }

  render() {
    const { passkeys, isLoading, displayError, displayAlert, expanded } = this.state;

    const passkeyList = passkeys.map(passkey => {
      return <tr key={passkey.id}>
        <th scope="row">{passkey.id}</th>
        <td style={{ whiteSpace: 'nowrap' }}>{passkey.label}</td>
        <td>{passkey.created}</td>
        <td>{passkey.lastUsed}</td>
        <td>{passkey.signatureCount}</td>
        <td>{passkey.lastModifiedByUser}</td>
        <td>{passkey.lastModifiedDate}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/webauthns/" + passkey.id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({ 'id': passkey.id, 'name': passkey.name })}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
          <Nav tabs>
        <NavItem>
          <NavLink
            className={classnames({ active: this.state.activeTab === '1' })}
            onClick={() => { this.toggle('1'); }}>
            Passkeys
        </NavLink>
        </NavItem>
      </Nav>
      <TabContent activeTab={this.state.activeTab}>
        <TabPane tabId="1">
          <div className="float-right"
                style={{
                  padding: '15px 20px 0 20px'
                }}
          >
            <Button color="success" tag={Link} to="/passkeys/new" disabled={displayAlert}>Add Passkey</Button>
          </div>
          <Table striped responsive>
            <thead>
              <tr>
                <th>#</th>
                <th>Label</th>
                <th>Created</th>
                <th>Last Used</th>
                <th>Signature Count</th>
                <th>Last Modified By User</th>
                <th>Last Modified By Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {passkeyList}
            </tbody>
          </Table>
        </TabPane>
      </TabContent>
      </div>
      }
    }

    return (
      <div
      style={{
        marginLeft: marginLeft(expanded),
        padding: '15px 20px 0 20px'
      }}
      >
        <AppNavbar />
        <Container fluid>
          <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
          {loading(isLoading)}
          {!isLoading && displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(PasskeyList);