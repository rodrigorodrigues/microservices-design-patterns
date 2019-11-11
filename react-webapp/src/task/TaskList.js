import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import { get } from "../services/ApiService";
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';

class TaskList extends Component {
  constructor(props) {
    super(props);
    this.state = { tasks: [], isLoading: true, jwt: props.jwt, displayError: null };
    this.remove = this.remove.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    if (jwt) {
      try {
        let data = await get('tasks', true, false, jwt);
        if (data) {
          if (Array.isArray(data)) {
            this.setState({ isLoading: false, tasks: data });
          } else {
            this.setState({ isLoading: false, displayError: errorMessage(data) });
          }
        }
      } catch (error) {
        this.setState({ displayError: errorMessage(error) });
      }
    }
  }

  async remove(task) {
    let confirm = await confirmDialog(`Delete Task ${task.name}`, "Are you sure you want to delete this?", "Delete Task");
    if (confirm) {
      let id = task.id;
      let jwt = this.state.jwt;
      await fetch(`/api/tasks/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt
        },
        credentials: 'include'
      }).then((err) => {
        if (err.status !== 200) {
          this.setState({ displayError: errorMessage(err) });
        } else {
          let tasks = [...this.state.tasks].filter(i => i.id !== id);
          this.setState({ tasks: tasks });
        }
      });
    }
  }

  render() {
    const { tasks, isLoading, displayError } = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const taskList = tasks.map(task => {
      return <tr key={task.id}>
        <td style={{ whiteSpace: 'nowrap' }}>{task.name}</td>
        <td>{task.createdByUser}</td>
        <td>{task.createdDate}</td>
        <td>{task.lastModifiedByUser}</td>
        <td>{task.lastModifiedDate}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/tasks/" + task.id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({ 'id': task.id, 'name': task.name })}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    return (
      <div>
        <AppNavbar />
        <Container fluid>
          <div className="float-right">
            <Button color="success" tag={Link} to="/tasks/new">Add Task</Button>
            <Button color="warning" tag={Link} to="/tasks/new">Try Swagger</Button>
          </div>
          <div className="float-left">
            <HomeContent notDisplayMessage={true}></HomeContent>
          </div>
          <div className="float-right">
            <h3>Tasks</h3>
            <Table className="mt-4">
              <thead>
                <tr>
                  <th width="40%">Name</th>
                  <th width="15%">Created By</th>
                  <th width="15%">Created Date</th>
                  <th width="15%">Last Modified By User</th>
                  <th width="15%">Last Modified By Date</th>
                </tr>
              </thead>
              <tbody>
                {taskList}
              </tbody>
            </Table>
            <MessageAlert {...displayError}></MessageAlert>
          </div>
        </Container>
      </div>
    );
  }
}

export default withRouter(TaskList);