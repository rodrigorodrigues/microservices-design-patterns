import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import ChildModal from "./child/ChildModal";
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';

class PersonList extends Component {
  constructor(props) {
    super(props);
    this.state = {persons: [], isLoading: true, jwt: props.jwt, displayError: null, authorities: props.authorities};
    this.remove = this.remove.bind(this);
  }

  componentDidMount() {
    console.log("JWT: ", this.state.jwt);
    if (this.state.jwt) {
      /*const eventSource = new EventSourcePolyfill('api/persons', {
        headers: {
          'Authorization': this.state.jwt
        }
      });*/
      const eventSource = new EventSource(`api/persons?Authorization=${this.state.jwt}`, {withCredentials: true});

      eventSource.addEventListener("open", result => {
        console.log('EventSource open: ', result);
        this.setState({isLoading: false});
      });

      eventSource.addEventListener("message", result => {
        console.log('EventSource result: ', result);
        const data = JSON.parse(result.data);
        this.state.persons.push(data);
        this.setState({persons: this.state.persons});
      });

      eventSource.addEventListener("error", err => {
        console.log('EventSource error: ', err);
        eventSource.close();
        if (err.status === 401 || err.status === 403) {
          this.props.onRemoveAuthentication();
          this.props.history.push('/');
        }
        if (this.state.persons.length === 0) {
          this.setState({displayError: errorMessage(err)});
        }
      });
    }
  }

  async remove(id) {
    await fetch(`/api/persons/${id}`, {
      method: 'DELETE',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      credentials: 'include'
    }).then(() => {
      let persons = [...this.state.persons].filter(i => i.id !== id);
      this.setState({persons: persons});
    });
  }

  render() {
    const {persons, isLoading, displayError, authorities} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_CREATE');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_SAVE');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_DELETE');

    const personList = persons.map(person => {
      const address = `${person.address.address || ''} ${person.address.city || ''} ${person.address.stateOrProvince || ''}`;
      return <tr key={person.id}>
        <td style={{whiteSpace: 'nowrap'}}>{person.fullName}</td>
        <td>{person.createdByUser}</td>
        <td>{person.dateOfBirth}</td>
        <td>{person.children ? <ChildModal person={person} /> : 'No Child'}</td>
        <td>{address}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/persons/" + person.id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove(person.id)} disabled={!hasDeleteAccess}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    return (
        <div>
          <AppNavbar/>
          <Container fluid>
            <div className="float-right">
              <Button color="success" tag={Link} to="/persons/new" disabled={!hasCreateAccess}>Add Person</Button>
            </div>
            <h3>Persons</h3>
            <Table className="mt-4">
              <thead>
              <tr>
                <th width="10%">Name</th>
                <th width="5%">Created By User</th>
                <th width="5%">Age</th>
                <th width="5%">Children List</th>
                <th width="15%">Location</th>
                <th width="10%">Actions</th>
              </tr>
              </thead>
              <tbody>
              {personList}
              </tbody>
            </Table>
            <MessageAlert {...displayError}></MessageAlert>
          </Container>
        </div>
    );
  }
}

export default withRouter(PersonList);