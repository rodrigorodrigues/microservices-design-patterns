import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table } from 'reactstrap';
import AppNavbar from './AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import ChildModal from "./ChildModal";

class PersonList extends Component {
  constructor(props) {
    super(props);
    this.state = {persons: [], isLoading: true};
    this.remove = this.remove.bind(this);
  }

  componentDidMount() {
    if (this.props.stateParent.jwt) {
      this.setState({isLoading: true});

      fetch('api/persons', {
        headers: {'Authorization': this.props.stateParent.jwt}
      })
          .then(response => response.json())
          .then(data => this.setState({persons: data, isLoading: false}))
          .catch(() => this.props.history.push('/'));
    }
  }

  async remove(id) {
    await fetch(`/api/persons/${id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': this.props.stateParent.jwt,
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      credentials: 'include'
    }).then(() => {
      let updatedGroups = [...this.state.persons].filter(i => i.id !== id);
      this.setState({persons: updatedGroups});
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