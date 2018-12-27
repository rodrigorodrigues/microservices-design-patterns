import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table } from 'reactstrap';
import AppNavbar from './AppNavbar';
import { Link, withRouter } from 'react-router-dom';

class PersonList extends Component {
  constructor(props) {
    super(props);
    this.state = {persons: [], isLoading: true};
    this.remove = this.remove.bind(this);
  }

  componentDidMount() {
    this.setState({isLoading: true});

    fetch('api/persons', {credentials: 'include'})
      .then(response => response.json())
      .then(data => this.setState({persons: data, isLoading: false}))
      .catch(() => this.props.history.push('/'));
  }

  async remove(id) {
    await fetch(`/api/persons/${id}`, {
      method: 'DELETE',
      headers: {
        'X-XSRF-TOKEN': this.state.csrfToken,
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
      const address = `${person.address || ''} ${person.city || ''} ${person.stateOrProvince || ''}`;
      return <tr key={person.id}>
        <td style={{whiteSpace: 'nowrap'}}>{person.name}</td>
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
          <h3>My JUG Tour</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="20%">Name</th>
              <th width="20%">Location</th>
              <th>Events</th>
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

export default withCookies(withRouter(PersonList));