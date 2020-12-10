import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import ChildModal from "./child/ChildModal";
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import { EventSourcePolyfill } from 'event-source-polyfill';
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import Iframe from 'react-iframe';
import { toast } from 'react-toastify';
import { marginLeft } from '../common/Util';

const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;
const personSwaggerUrl = process.env.REACT_APP_PERSON_SWAGGER_URL;

class PersonList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      persons: [],
      isLoading: true,
      jwt: props.jwt,
      displayError: null,
      authorities: props.authorities,
      displayAlert: false,
      displaySwagger: false,
      activeTab: '1',
      isAuthenticated: props.isAuthenticated,
      user: props.user,
      expanded: false
    };
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

  componentDidMount() {
    toast.dismiss('Error');
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_READ' 
      || item === 'ROLE_PERSON_READ' || item === 'ROLE_PERSON_CREATE' 
      || item === 'ROLE_PERSON_SAVE' || item === 'ROLE_PERSON_DELETE')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        const eventSource = new EventSourcePolyfill(`${gatewayUrl}/api/people`, {
          headers: {
            'Authorization': jwt
          }
        });

        eventSource.addEventListener("open", result => {
          console.log('EventSource open: ', result);
          this.setState({ isLoading: false, displaySwagger: true });
        });

        eventSource.addEventListener("message", result => {
          const data = JSON.parse(result.data);
          this.state.persons.push(data);
          this.setState({ persons: this.state.persons });
        });

        eventSource.addEventListener("error", err => {
          console.log('EventSource error: ', err);
          eventSource.close();
          this.setState({ isLoading: false });
          if (err.status === 401) {
            this.setState({ displayAlert: true });
          } else {
            if (this.state.persons.length === 0) {
              this.setState({ displayError: errorMessage(JSON.stringify(err)) });
            }
          }
        });
      }
    }
  }

  async remove(person) {
    let confirm = await confirmDialog(`Delete Person ${person.name}`, "Are you sure you want to delete this?", "Delete Person");
    if (confirm) {
      let id = person.id;
      let jwt = this.state.jwt;
      await fetch(`/api/people/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt
        },
        credentials: 'include'
      }).then((err) => {
        if (err.status !== 200) {
          this.setState({ displayError: errorMessage(err) });
        } else {
          let persons = [...this.state.persons].filter(i => i.id !== id);
          this.setState({ persons: persons });
        }
      });
    }
  }

  render() {
    const { persons, isLoading, displayError, authorities, displayAlert, displaySwagger, expanded } = this.state;

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_CREATE');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_SAVE');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_DELETE');

    const personList = persons.map(person => {
      const address = `${person.address.address || ''} ${person.address.city || ''} ${person.address.stateOrProvince || ''}`;
      return <tr key={person.id}>
        <th scope="row">{person.id}</th>
        <td style={{ whiteSpace: 'nowrap' }}>{person.fullName}</td>
        <td>{person.createdByUser}</td>
        <td>{person.createdDate}</td>
        <td>{person.lastModifiedByUser}</td>
        <td>{person.lastModifiedDate}</td>
        <td>{person.dateOfBirth}</td>
        <td>{person.children ? <ChildModal person={person} /> : 'No Child'}</td>
        <td>{address}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/people/" + person.id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({ 'id': person.id, 'name': person.fullName })} disabled={!hasDeleteAccess}>Delete</Button>
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
                Persons
              </NavLink>
            </NavItem>
            <NavItem>
              <NavLink
                className={classnames({ active: this.state.activeTab === '2' })}
                onClick={() => { this.toggle('2'); }}>
                Swagger UI
              </NavLink>
            </NavItem>
          </Nav>
          <TabContent activeTab={this.state.activeTab}>
            <TabPane tabId="1">
              <div className="float-right">
                <Button color="success" tag={Link} to="/people/new" disabled={!hasCreateAccess || displayAlert}>Add Person</Button>
              </div>
              <Table striped responsive>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Name</th>
                    <th>Created By</th>
                    <th>Created Date</th>
                    <th>Last Modified By User</th>
                    <th>Last Modified By Date</th>
                    <th>Date of Birth</th>
                    <th>Children List</th>
                    <th>Location</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {personList}
                </tbody>
              </Table>
            </TabPane>
            <TabPane tabId="2">
              {displaySwagger ?
                <Iframe url={`${personSwaggerUrl}`}
                  position="absolute"
                  width="100%"
                  id="myId"
                  className="mt-4"
                  height="100%" />
                : null}
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
          {isLoading && <i class="fa fa-spinner" aria-hidden="true"></i>}
          {!isLoading && displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(PersonList);