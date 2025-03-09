import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {Feedback, Form, FormGroup, Input, Label} from '@availity/form';
import * as yup from 'yup';
import {Button, Container, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import FooterContent from '../home/FooterContent';
import HomeContent from '../home/HomeContent';
import { marginLeft } from '../common/Util';
import { loading } from '../common/Loading';
import uuid from 'react-uuid';

class TaskEdit extends Component {
  emptyTask = {
    name: '',
    requestId: uuid()
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
      isAuthenticated: props.isAuthenticated,
      gatewayUrl: props.gatewayUrl
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      const { jwt, authorities, gatewayUrl } = this.state;
      if (jwt && authorities) {
        if (!authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_CREATE' || item === 'ROLE_TASK_SAVE' || item === 'SCOPE_openid')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          if (this.props.match.params.id !== 'new') {
            try {
              const task = await (await fetch(`${gatewayUrl}/api/tasks/${this.props.match.params.id}`, { method: 'GET',      headers: {
                'Content-Type': 'application/json',
                'Authorization': jwt,
                'requestId': uuid()
              }})).json();
              this.setState({task: task, isLoading: false});
            } catch (error) {
              this.setState({displayAlert: true, sLoading: false, displayError: errorMessage(error)});
            }
          } else {
            this.setState({isLoading: false});
          }
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

  async handleSubmit(event) {
    try {
      const { task, jwt, gatewayUrl }  = this.state;
      console.log("Task", task);

      const url = `${gatewayUrl}/api/tasks` + (task.id ? '/' + task.id : '');
      await fetch(url, {
        method: (task.id) ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': jwt
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

  render() {
    const { task, displayError, displayAlert, isLoading, expanded } = this.state;
    const title = <h2>{task.id ? 'Edit Task' : 'Add Task'}</h2>;

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
              name: task?.name || ''
            }}
            validationSchema={yup.object().shape({
              name: yup.string().trim().required('This field is required.')
            })}
          >
            <FormGroup>
              <Label for="name">Name</Label>
              <Input type="text" name="name" id="name" onChange={this.handleChange} placeholder="Name" />
              <Feedback name="name" />
            </FormGroup>
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

export default withRouter(TaskEdit);