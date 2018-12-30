import React, { Component } from 'react';
import { Link, withRouter } from 'react-router-dom';
import {Alert, Button, Container, Form, FormFeedback, FormGroup, Input, Label} from 'reactstrap';
import AppNavbar from './AppNavbar';
import ReactJson from 'react-json-view'

class PersonEdit extends Component {
  emptyPerson = {
    name: '',
    address: {
      address: '',
      city: '',
      stateOrProvince: '',
      country: '',
      postalCode: ''
    }
  };

  constructor(props) {
    super(props);
    this.state = {
      person: this.emptyPerson,
      displayError: null,
      validate: {
        emailState: '',
      }
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  async componentDidMount() {
    if (this.props.match.params.id !== 'new' && this.props.stateParent.jwt) {
      try {
        const person = await (await fetch(`/api/persons/${this.props.match.params.id}`,
            {headers: {'Authorization': this.props.stateParent.jwt}})).json();
        this.setState({person: person});
      } catch (error) {
        this.setState({displayError: error});
      }
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let person = {...this.state.person};
    person[name] = value;
    this.setState({person: person});
  }

  async handleSubmit(event) {
    event.preventDefault();
    const {person} = this.state;

    await fetch('/api/persons', {
      method: (person.id) ? 'PUT' : 'POST',
      headers: {
        'Authorization': this.props.stateParent.jwt,
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(person),
      credentials: 'include'
    });
    this.props.history.push('/persons');
  }

  validateEmail(e) {
    const emailRex = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    const { validate } = this.state
    if (emailRex.test(e.target.value)) {
      validate.emailState = 'has-success'
    } else {
      validate.emailState = 'has-danger'
    }
    this.setState({ validate })
  }

  render() {
    const displayError = this.state.displayError != null ?
        <div>
          <Alert color="danger">
            <ReactJson enableClipboard={false} displayObjectSize={false} name={null} displayDataTypes={false} src={this.state.displayError} />
          </Alert>
        </div>: '';

    const {person} = this.state;
    const title = <h2>{person.id ? 'Edit Person' : 'Add Person'}</h2>;

    return <div>
      <AppNavbar/>
      <Container>
        {title}
        <Form onSubmit={this.handleSubmit}>
          <FormGroup>
            <Label for="name">Name</Label>
            <Input type="text" name="name" id="name" value={person.name || ''}
                   onChange={this.handleChange} placeholder="Full Name"/>
          </FormGroup>
          <FormGroup>
            <Label for="age">Age</Label>
            <Input type="text" name="age" id="age" value={person.age || ''}
                   onChange={this.handleChange} placeholder="Age"/>
          </FormGroup>
          <FormGroup>
            <Label for="name">Email</Label>
            <Input type="text" name="email" id="email" value={person.email || ''}
                   invalid={ this.state.validate.emailState === 'has-danger' }
                   onChange={ e => {
                     this.validateEmail(e);
                     this.handleChange(e);
                   }} placeholder="your_email@email.com"/>
            <FormFeedback>
              Looks like there is an issue with your email. Please input a correct email.
            </FormFeedback>
          </FormGroup>
          <FormGroup>
            <Label for="address">Address</Label>
            <Input type="text" name="address" id="address" value={person.address.address || ''}
                   onChange={this.handleChange} placeholder="Address"/>
          </FormGroup>
          <div className="row">
            <FormGroup className="col-md-4 mb-3">
              <Label for="city">City</Label>
              <Input type="text" name="city" id="city" value={person.address.city || ''}
                     onChange={this.handleChange} placeholder="City"/>
            </FormGroup>
            <FormGroup className="col-md-4 mb-3">
              <Label for="stateOrProvince">State/Province</Label>
              <Input type="text" name="stateOrProvince" id="stateOrProvince" value={person.address.stateOrProvince || ''}
                     onChange={this.handleChange} placeholder="State/Province"/>
            </FormGroup>
            <FormGroup className="col-md-4 mb-3">
              <Label for="country">Country</Label>
              <Input type="text" name="country" id="country" value={person.address.country || ''}
                     onChange={this.handleChange} placeholder="Country"/>
            </FormGroup>
            <FormGroup className="col-md-4 mb-3">
              <Label for="country">Postal Code</Label>
              <Input type="text" name="postalCode" id="postalCode" value={person.address.postalCode || ''}
                     onChange={this.handleChange} placeholder="Postal Code"/>
            </FormGroup>
          </div>
          <FormGroup>
            <Button color="primary" type="submit">Save</Button>{' '}
            <Button color="secondary" tag={Link} to="/persons">Cancel</Button>
          </FormGroup>
          {displayError}
        </Form>
      </Container>
    </div>
  }
}

export default withRouter(PersonEdit);