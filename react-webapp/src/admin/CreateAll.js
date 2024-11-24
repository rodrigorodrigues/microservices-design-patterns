import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {Feedback, Form, FormGroup, Input} from '@availity/form';
import * as yup from 'yup';
import {Row, Col, Button, Container, Label, UncontrolledAlert, Card, CardBody, CardTitle, CardSubtitle, CardText, CardLink} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import FooterContent from '../home/FooterContent';
import HomeContent from '../home/HomeContent';
import { marginLeft } from '../common/Util';
import { loading } from '../common/Loading';
import uuid from 'react-uuid';
import AsyncSelect from 'react-select/async';
import { get } from "../services/ApiService";
import DatePicker from "react-datepicker";
const moment = require('moment');

class CreateAll extends Component {

  emptyTask = {
    requestId: uuid(),
    name: '',
    fullName: '',
    dateOfBirth: '',
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
      task: this.emptyTask,
      jwt: props.jwt,
      displayError: null,
      displayAlert: false,
      authorities: props.authorities,
      isLoading: false,
      expanded: false,
      isAuthenticated: props.isAuthenticated
    };
    this.handleChange = this.handleChange.bind(this);
    this.handlePersonChange = this.handlePersonChange.bind(this);
    this.handlePersonInputChange = this.handlePersonInputChange.bind(this);
    this.handleDateChange = this.handleDateChange.bind(this);
    this.handleAddressChange = this.handleAddressChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      let jwt = this.state.jwt;
      let permissions = this.state.authorities;
      if (jwt && permissions) {

        if (!permissions.some(item => item === 'ROLE_ADMIN')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let task = {...this.state.task};
    task[name] = value;
    this.setState({task: task});
  }

  handlePersonChange(event) {
    if (event !== "") {
      window.localStorage.setItem('personName', event);
    }
  }

  handlePersonInputChange(event) {
    if (event !== undefined) {
      let task = {...this.state.task};
      task.address = event.address;
      task.fullName = event.fullName + " Copy";
      task.dateOfBirth = event.dateOfBirth;
      this.setState({task: task});
    }
  }

  handlePostChange(event) {
    window.localStorage.setItem('postName', event);
  }

  handleAddressChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let address = {...this.state.task.address};
    address[name] = value;
    let task = {...this.state.task};
    task.address = address;
    this.setState({task: task});
  }

  handleDateChange(date) {
    let task = {...this.state.task};
    task.dateOfBirth = moment(date).format("YYYY-MM-DD");
    this.setState({
      task: task
    });
  }

  async handleSubmit(event) {
    try {
      const {task, jwt} = this.state;
      console.log("Task", task);

      const url = '/api/tasks' + (task.id ? '/' + task.id : '');
      await fetch(url, {
        method: (task.id) ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': jwt,
          'requestId': uuid()
        },
        body: JSON.stringify(task),
        credentials: 'include'
      }).then(response => response.json())
          .then(data => {
            if (data.id) {
              this.props.history.push('/tasks');
            } else {
              this.setState({ displayError: errorMessage(data)});
            }
          })
          .catch((error) => {
            this.setState({ displayError: errorMessage(error)});
          });
    } finally {
      this.setLoading(false);
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  async loadPersonOptions() {
    let search = window.localStorage.getItem('personName');
    console.log(`inputValue: ${search}`);
    if (search !== "") {
      let url = `people?${search}`;

      let jwt = window.localStorage.getItem('jhi-authenticationToken');
      let data = await get(url, true, false, jwt);
      return data.content;
    }
  }

  async loadPostOptions() {
    let search = window.localStorage.getItem('postName');
    console.log(`inputValue: ${search}`);
    if (search !== "") {
      let url = `postsByName?${search}`;

      let jwt = window.localStorage.getItem('jhi-authenticationToken');
      let data = await get(url, true, false, jwt);
      return data.content;
    }
  }

  render() {
    const { task, displayError, displayAlert, isLoading, expanded, selectedValue } = this.state;
    const title = <h2>{task.id ? 'Edit All' : 'Add All'}</h2>;

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
          {title}
          <Form 
            onSubmit={this.handleSubmit}
            initialValues={{ 
              fullName: task?.fullName || ''
            }}
            validationSchema={yup.object().shape({
              fullName: yup.string().trim().required(),
              dateOfBirth: yup.string().trim().required()
            })}
          >
          <Row>
  <Col>
     <Card>
      <CardBody>
        <CardTitle tag="h5">Add Person</CardTitle>
        <CardSubtitle className="mb-2 text-muted">
          <FormGroup>
            <Label for="person">Copy Person:</Label>
            <AsyncSelect
              cacheOptions
              defaultOptions
              value={selectedValue}
              getOptionLabel={e => e.fullName}
              getOptionValue={e => e.id}
              loadOptions={this.loadPersonOptions}
              onInputChange={this.handlePersonChange}
              onChange={this.handlePersonInputChange}
            />
          </FormGroup>
        </CardSubtitle>
        <CardText>
        <FormGroup>
              <Label for="fullName">Full Name</Label>
              <Input type="text" name="fullName" id="fullName" 
                    onChange={this.handleChange} placeholder="Full Name"
                      required
                      minLength="5"
                      maxLength="200"
                      pattern="^[A-Za-z0-9\s+]+$" />
              <Feedback name="fullName">
                This field is invalid - Your Full Name must be between 5 and 100 characters.
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Label for="dateOfBirth">Date Of Birth</Label>
              <DatePicker
                name="dateOfBirth"
                selected={task.dateOfBirth !== '' ? moment(task.dateOfBirth).toDate() : ''}
                onChange={this.handleDateChange}
                required
              />
            </FormGroup>
            <FormGroup>
              <Label for="address">Address</Label>
              <Input type="text" name="address" id="address" value={task.address.address || ''}
                    onChange={this.handleAddressChange} placeholder="Address"/>
            </FormGroup>
            <div className="row">
              <FormGroup className="col-md-4 mb-3">
                <Label for="city">City</Label>
                <Input type="text" name="city" id="city" value={task.address.city || ''}
                      onChange={this.handleAddressChange} placeholder="City"/>
              </FormGroup>
              <FormGroup className="col-md-4 mb-3">
                <Label for="stateOrProvince">State/Province</Label>
                <Input type="text" name="stateOrProvince" id="stateOrProvince" value={task.address.stateOrProvince || ''}
                      onChange={this.handleAddressChange} placeholder="State/Province"/>
              </FormGroup>
              <FormGroup className="col-md-4 mb-3">
                <Label for="country">Country</Label>
                <Input type="text" name="country" id="country" value={task.address.country || ''}
                      onChange={this.handleAddressChange} placeholder="Country"/>
              </FormGroup>
              <FormGroup className="col-md-4 mb-3">
                <Label for="country">Postal Code</Label>
                <Input type="text" name="postalCode" id="postalCode" value={task.address.postalCode || ''}
                      onChange={this.handleAddressChange} placeholder="Postal Code"/>
              </FormGroup>
            </div>
        </CardText>
      </CardBody>
    </Card>
</Col>
<Col>

    <Card>
      <CardBody>
        <CardTitle tag="h5">
          Add Task
        </CardTitle>
        <CardSubtitle
          className="mb-2 text-muted"
          tag="h6"
        >
          <FormGroup>
            <Label for="name">Task Name</Label>
            <Input type="text" name="name" id="name" value={task.name || ''}
                  onChange={this.handleChange} placeholder="Name"
                    required/>
            <Feedback>
              This field is invalid
            </Feedback>
          </FormGroup>
        </CardSubtitle>
        <CardText>
        <FormGroup>
            <Label for="post">Select Post:</Label>
            <AsyncSelect
              cacheOptions
              defaultOptions
              value={selectedValue}
              getOptionLabel={e => e.name}
              getOptionValue={e => e.id}
              loadOptions={this.loadPostOptions}
              onInputChange={this.handlePostChange}
              onChange={this.handlePostChange}
            />
          </FormGroup>
        </CardText>
      </CardBody>
    </Card>
                
    </Col>
</Row>

            <FormGroup>
              <Button color="primary" type="submit">{task.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/tasks">Cancel</Button>
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
      <Container fluid>
        <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
        {loading(isLoading)}
        {!isLoading && displayContent()}
        <MessageAlert {...displayError}></MessageAlert>
        <FooterContent></FooterContent>
      </Container>
    </div>
  }
}

export default withRouter(CreateAll);