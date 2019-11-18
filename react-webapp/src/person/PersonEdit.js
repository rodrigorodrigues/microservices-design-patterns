import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {AvFeedback, AvForm, AvInput, AvGroup} from 'availity-reactstrap-validation';
import {Button, Container, Label, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import DatePicker from "react-datepicker";
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';
import "react-datepicker/dist/react-datepicker.css";
const moment = require('moment');

class PersonEdit extends Component {
  emptyPerson = {
    fullName: '',
    dateOfBirth: moment().format("YYYY-MM-DD"),
    /*children: [{
      name: '',
      dateOfBirth: ''
    }]*/
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
      jwt: props.jwt,
      displayAlert: false,
      authorities: props.authorities
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleDateChange = this.handleDateChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleAddressChange = this.handleAddressChange.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_CREATE' || item === 'ROLE_PERSON_SAVE')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        return;
      } else {
        if (this.props.match.params.id !== 'new') {
          try {
            const person = await (await fetch(`/api/persons/${this.props.match.params.id}`, { method: 'GET',      headers: {
              'Content-Type': 'application/json',
              'Authorization': jwt
            }})).json();
            this.setState({person: person});
          } catch (error) {
            this.setState({ displayError: errorMessage(error)});
          }
        }
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

  handleAddressChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let address = {...this.state.person.address};
    address[name] = value;
    let person = {...this.state.person};
    person.address = address;
    this.setState({person: person});
  }

  handleDateChange(date) {
    console.log("handleDateChange", date);
    let person = {...this.state.person};
    person.dateOfBirth = moment(date).format("YYYY-MM-DD");
    console.log("handleDateChange:person", person);
    this.setState({
      person: person
    });
  }

  async handleSubmit(event) {
    event.preventDefault();
    const {person, jwt} = this.state;
    console.log("handleSubmit:person", person);
    const url = '/api/persons' + (person.id ? '/' + person.id : '');

    await fetch(url, {
      method: (person.id) ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': jwt
      },
      body: JSON.stringify(person),
      credentials: 'include'
    }).then(response => response.json())
        .then(data => {
          if (data.id) {
            this.props.history.push('/persons');
          } else {
            this.setState({ displayError: errorMessage(data)});
          }
        })
        .catch((error) => {
          console.log(error);
        });
  }

  render() {
    const {person, displayError, displayAlert, isLoading} = this.state;
    const title = <h2>{person.id ? 'Edit Person' : 'Add Person'}</h2>;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
          401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
        </UncontrolledAlert>
      } else {
        return <div>
          {title}
          <AvForm onValidSubmit={this.handleSubmit}>
            <AvGroup>
              <Label for="fullName">Full Name</Label>
              <AvInput type="text" name="fullName" id="fullName" value={person.fullName || ''}
                    onChange={this.handleChange} placeholder="Full Name"
                      required
                      minLength="5"
                      maxLength="200"
                      pattern="^[A-Za-z0-9\s+]+$" />
              <AvFeedback>
                This field is invalid - Your Full Name must be between 5 and 100 characters.
              </AvFeedback>
            </AvGroup>
            <AvGroup>
              <Label for="dateOfBirth">Date Of Birth</Label>
              <DatePicker
                selected={person.dateOfBirth}
                onChange={this.handleDateChange}
                required
              />
            </AvGroup>
            <AvGroup>
              <Label for="address">Address</Label>
              <AvInput type="text" name="address" id="address" value={person.address.address || ''}
                    onChange={this.handleAddressChange} placeholder="Address"/>
            </AvGroup>
            <div className="row">
              <AvGroup className="col-md-4 mb-3">
                <Label for="city">City</Label>
                <AvInput type="text" name="city" id="city" value={person.address.city || ''}
                      onChange={this.handleAddressChange} placeholder="City"/>
              </AvGroup>
              <AvGroup className="col-md-4 mb-3">
                <Label for="stateOrProvince">State/Province</Label>
                <AvInput type="text" name="stateOrProvince" id="stateOrProvince" value={person.address.stateOrProvince || ''}
                      onChange={this.handleAddressChange} placeholder="State/Province"/>
              </AvGroup>
              <AvGroup className="col-md-4 mb-3">
                <Label for="country">Country</Label>
                <AvInput type="text" name="country" id="country" value={person.address.country || ''}
                      onChange={this.handleAddressChange} placeholder="Country"/>
              </AvGroup>
              <AvGroup className="col-md-4 mb-3">
                <Label for="country">Postal Code</Label>
                <AvInput type="text" name="postalCode" id="postalCode" value={person.address.postalCode || ''}
                      onChange={this.handleAddressChange} placeholder="Postal Code"/>
              </AvGroup>
            </div>
            <AvGroup>
              <Button color="primary" type="submit">{person.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/persons">Cancel</Button>
            </AvGroup>
          </AvForm>
          </div>
      }
    }

    return <div>
      <AppNavbar/>
      <HomeContent notDisplayMessage={true}></HomeContent>
      <Container fluid>
      {displayContent()}
      <MessageAlert {...displayError}></MessageAlert>
      <FooterContent></FooterContent>
      </Container>
    </div>
  }
}

export default withRouter(PersonEdit);