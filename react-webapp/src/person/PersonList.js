import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import ChildModal from "./child/ChildModal";
import { EventSourcePolyfill } from 'event-source-polyfill';

class PersonList extends Component {
  constructor(props) {
    super(props);
    this.state = {persons: [], isLoading: true};
    this.remove = this.remove.bind(this);
  }

  componentDidMount() {
    let jwt = this.props.jwt;
    if (jwt) {
      this.eventSource = new EventSourcePolyfill('api/persons', {
        headers: {
          'Authorization': jwt,
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache',
          'Access-Control-Allow-Origin': '*'
        }
      });
      this.eventSource.onmessage = result => {
        const data = JSON.parse(result.data);
        this.state.persons.push(data);
        this.setState({persons: this.state.persons, isLoading: false})
      };

      this.eventSource.onerror = err => {
        console.log('EventSource error: ', err);
        this.eventSource.close();
        //this.props.history.push('/');
      };
/*      fetch('api/persons', {
        headers: {'Authorization': this.props.jwt}
      })
          .then(response => response.json())
          .then(data => this.setState({persons: data, isLoading: false}))
          .catch(() => this.props.history.push('/'));*/
    }
  }

  async remove(id) {
    await fetch(`/api/persons/${id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': this.props.jwt,
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
    const {persons, isLoading} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const personList = persons.map(person => {
      const address = `${person.address.address || ''} ${person.address.city || ''} ${person.address.stateOrProvince || ''}`;
      return <tr key={person.id}>
        <td style={{whiteSpace: 'nowrap'}}>{person.name}</td>
        <td>{person.login}</td>
        <td>{person.age}</td>
        <td>{person.children ? <ChildModal person={person} /> : 'No Child'}</td>
        <td>{address}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/persons/" + person.id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove(person.id)}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    return (
      <div>
        <AppNavbar/>
        <Container fluid>
          <div className="float-right">
            <Button color="success" tag={Link} to="/persons/new">Add Person</Button>
          </div>
          <h3>Persons</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="10%">Name</th>
              <th width="5%">Login</th>
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
        </Container>
      </div>
    );
  }
}

export default withRouter(PersonList);