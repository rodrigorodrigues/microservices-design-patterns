import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import ChildModal from "./child/ChildModal";
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import { EventSourcePolyfill } from 'event-source-polyfill';
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';

const gatewayUrl = process.env.REACT_APP_GATEWAY_URL;

class PersonList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      persons: [], 
      isLoading: true, 
      jwt: props.jwt, 
      displayError: null, 
      authorities: props.authorities,
      displayAlert: false
    };
    this.remove = this.remove.bind(this);
  }

  componentDidMount() {
    let jwt = this.state.jwt;
    if (jwt) {
      const eventSource = new EventSourcePolyfill(`${gatewayUrl}/api/persons`, {
        headers: {
          'Authorization': jwt
        }
      });

      eventSource.addEventListener("open", result => {
        console.log('EventSource open: ', result);
        this.setState({isLoading: false});
      });

      eventSource.addEventListener("message", result => {
        const data = JSON.parse(result.data);
        this.state.persons.push(data);
        this.setState({persons: this.state.persons});
      });

      eventSource.addEventListener("error", err => {
        console.log('EventSource error: ', err);
        eventSource.close();
        this.setState({isLoading: false});
        if (err.status === 401) {
          this.setState({displayAlert: true});
        } else {
          if (this.state.persons.length === 0) {
            this.setState({displayError: errorMessage(JSON.stringify(err))});
          }
        }
      });
    }
  }

  async remove(person) {
    let confirm = await confirmDialog(`Delete Person ${person.name}`, "Are you sure you want to delete this?", "Delete Person");
    if (confirm) {
      let id = person.id;
      let jwt = this.state.jwt;
      await fetch(`/api/persons/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt
        },
        credentials: 'include'
      }).then((err) => {
        if (err.status !== 200) {
          this.setState({displayError: errorMessage(err)});
        } else {
          let persons = [...this.state.persons].filter(i => i.id !== id);
          this.setState({persons: persons});
        }
      });
    }
  }
  
  render() {
    const {persons, isLoading, displayError, authorities, displayAlert} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_CREATE');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_SAVE');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_DELETE');

    const personList = persons.map(person => {
      const address = `${person.address.address || ''} ${person.address.city || ''} ${person.address.stateOrProvince || ''}`;
      return <tr key={person.id}>
        <th scope="row">{person.id}</th>
        <td style={{whiteSpace: 'nowrap'}}>{person.fullName}</td>
        <td>{person.createdByUser}</td>
        <td>{person.dateOfBirth}</td>
        <td>{person.children ? <ChildModal person={person} /> : 'No Child'}</td>
        <td>{address}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/persons/" + person.id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': person.id, 'name': person.fullName})} disabled={!hasDeleteAccess}>Delete</Button>
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
        return <Table striped responsive>
        <thead>
        <tr>
          <th>#</th>
          <th>Name</th>
          <th>Created By User</th>
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
      }
    }

    return (
        <div>
          <AppNavbar/>
          <Container fluid>
            <HomeContent notDisplayMessage={true}></HomeContent>
            <br />
            <div className="float-right">
              <Button color="success" tag={Link} to="/persons/new" disabled={!hasCreateAccess || displayAlert}>Add Person</Button>
            </div>
            <div className="float-left">
            <h3>Persons</h3>
            {displayContent()}
            <MessageAlert {...displayError}></MessageAlert>
            </div>
          </Container>
        </div>
    );
  }
}

export default withRouter(PersonList);