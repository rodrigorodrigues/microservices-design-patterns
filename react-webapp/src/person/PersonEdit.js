import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {Feedback, Form, Input, FormGroup, Label} from '@availity/form';
import * as yup from 'yup';
import {Button, Container, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import DatePicker from "react-datepicker";
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';
import "react-datepicker/dist/react-datepicker.css";
import { marginLeft } from '../common/Util';
import uuid from 'react-uuid';

const moment = require('moment');

class PersonEdit extends Component {
  emptyPerson = {
    fullName: '',
    dateOfBirth: moment().subtract(10, 'years').format("YYYY-MM-DD"),
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
      authorities: props.authorities,
      expanded: false,
      isAuthenticated: props.isAuthenticated
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleDateChange = this.handleDateChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleAddressChange = this.handleAddressChange.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_CREATE' || item === 'ROLE_PERSON_SAVE' || item === 'SCOPE_openid')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        if (this.props.match.params.id !== 'new') {
          try {
            const person = await (await fetch(`/api/people/${this.props.match.params.id}`, { method: 'GET',      headers: {
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
    const {person, jwt} = this.state;
    console.log("handleSubmit:person", person);
    const url = '/api/people' + (person.id ? '/' + person.id : '');

    await fetch(url, {
      method: (person.id) ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': jwt,
        'requestId': uuid()
      },
      body: JSON.stringify(person),
      credentials: 'include'
    }).then(response => response.json())
        .then(data => {
          if (data.id) {
            this.props.history.push('/people');
          } else {
            this.setState({ displayError: errorMessage(data)});
          }
        })
        .catch((error) => {
          console.log(error);
        });
  }

  render() {
    const { person, displayError, displayAlert, isLoading, expanded} = this.state;
    const title = <h2>{person.id ? 'Edit Person' : 'Add Person'}</h2>;

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
          401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
        </UncontrolledAlert>
      } else {
        return <div>
          {title}
          <Form onSubmit={this.handleSubmit}
            enableReinitialize={true}
            initialValues={{
              fullName: person?.fullName || '',
              dateOfBirth: moment(person.dateOfBirth).toDate(),
              address: person?.address?.address || '',
              city: person?.address?.city || '',
              stateOrProvince: person?.address?.stateOrProvince || '',
              country: person?.address?.country || ''
            }}
            validationSchema={yup.object().shape({
              fullName: yup.string().trim().required()
                .matches('^[A-Za-z0-9\s+]+$')
                .min(5)
                .max(200),
              dateOfBirth: yup.date().required(),
              address: yup.string().trim().required(),
              city: yup.string().trim().required(),
              stateOrProvince: yup.string().trim().required(),
              country: yup.string().trim().required(),
              postalCode: yup.string().trim().required()
            })}
          >
            <FormGroup>
              <Label for="fullName">Full Name</Label>
              <Input type="text" name="fullName" id="fullName" 
                    onChange={this.handleChange} placeholder="Full Name" />
              <Feedback name="fullName">
                This field is invalid - Your Full Name must be between 5 and 200 characters.
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="dateOfBirth">Date Of Birth</Label>
              <DatePicker
                name ="dateOfBirth"
                selected={moment(person.dateOfBirth).toDate()}
                onChange={this.handleDateChange}
              />
              <Feedback name="dateOfBirth" />
            </FormGroup>
            <FormGroup>
              <Label for="address">Address</Label>
              <Input type="text" name="address" id="address" onChange={this.handleAddressChange} placeholder="Address"/>
              <Feedback name="address" />
            </FormGroup>
            <div className="row">
              <FormGroup className="col-md-4 mb-3">
                <Label for="city">City</Label>
                <Input type="text" name="city" id="city" onChange={this.handleAddressChange} placeholder="City"/>
                <Feedback name="city" />
              </FormGroup>
              <FormGroup className="col-md-4 mb-3">
                <Label for="stateOrProvince">State/Province</Label>
                <Input type="text" name="stateOrProvince" id="stateOrProvince" onChange={this.handleAddressChange} placeholder="State/Province"/>
                <Feedback name="stateOrProvince" />
              </FormGroup>
              <FormGroup className="col-md-4 mb-3">
                <Label for="country">Country</Label>
                <Input type="text" name="country" id="country" onChange={this.handleAddressChange} placeholder="Country"/>
                <Feedback name="country" />
              </FormGroup>
              <FormGroup className="col-md-4 mb-3">
                <Label for="postalCode">Postal Code</Label>
                <Input type="text" name="postalCode" id="postalCode" value={person.address.postalCode || ''}
                      onChange={this.handleAddressChange} placeholder="Postal Code"/>
                <Feedback name="postalCode" />
              </FormGroup>
            </div>
            <FormGroup>
              <Button color="primary" type="submit">{person.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/people">Cancel</Button>
            </FormGroup>
          </Form>
          </div>
      }
    }

    return <div
    style={{
      marginLeft: marginLeft(expanded),
      padding: '15px 20px 0 20px'
    }}
    >
      <AppNavbar/>
      <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
      <Container fluid>
      {isLoading && <i class="fa fa-spinner" aria-hidden="true"></i>}
      {!isLoading && displayContent()}
      <MessageAlert {...displayError}></MessageAlert>
      <FooterContent></FooterContent>
      </Container>
    </div>
  }
}

export default withRouter(PersonEdit);