import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {AvFeedback, AvForm, AvGroup, AvInput} from 'availity-reactstrap-validation';
import {Button, Container, Label, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import FooterContent from '../home/FooterContent';
import HomeContent from '../home/HomeContent';

class TaskEdit extends Component {
  emptyTask = {
    name: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      task: this.emptyTask,
      jwt: props.jwt,
      displayError: null,
      displayAlert: false,
      authorities: props.authorities,
      isLoading: true
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  async componentDidMount() {
    if (!this.state.authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_CREATE' || item === 'ROLE_TASK_SAVE')) {
      const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
      this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      return;
    }

    if (this.props.match.params.id !== 'new') {
      try {
        let jwt = this.state.jwt;
        const task = await (await fetch(`/api/tasks/${this.props.match.params.id}`, { method: 'GET',      headers: {
          'Content-Type': 'application/json',
          'Authorization': jwt
        }})).json();
        this.setState({task: task, isLoading: false});
      } catch (error) {
        this.setState({displayAlert: true, sLoading: false, displayError: errorMessage(error)});
      }
    } else {
      this.setState({isLoading: false});
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
    event.preventDefault();
    const {task, jwt} = this.state;
    console.log("Task", task);
    console.log("Task jwt", jwt);

    const url = '/api/tasks' + (task.id ? '/' + task.id : '');
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
          console.log(error);
        });
  }

  render() {
    const {task, displayError, displayAlert, isLoading} = this.state;
    const title = <h2>{task.id ? 'Edit Task' : 'Add Task'}</h2>;

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
              <Label for="name">Name</Label>
              <AvInput type="text" name="name" id="name" value={task.name || ''}
                    onChange={this.handleChange} placeholder="Name"
                      required/>
              <AvFeedback>
                This field is invalid
              </AvFeedback>
            </AvGroup>
            <AvGroup>
              <Button color="primary" type="submit">{task.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/tasks">Cancel</Button>
            </AvGroup>
          </AvForm>
          </div>
      }
    }

    return <div>
      <AppNavbar/>
      <Container fluid>
        <HomeContent notDisplayMessage={true}></HomeContent>
        {displayContent()}
        <MessageAlert {...displayError}></MessageAlert>
        <FooterContent></FooterContent>
      </Container>
    </div>
  }
}

export default withRouter(TaskEdit);