import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert, Form, FormGroup, Input, Label } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import { get } from "../services/ApiService";
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import Iframe from 'react-iframe';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';
import { marginLeft } from '../common/Util';
import Pagination from "react-js-pagination";

const taskSwaggerUrl = process.env.REACT_APP_TASK_SWAGGER_URL;

class TaskList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      tasks: [],
      isLoading: true,
      jwt: props.jwt,
      displayError: null,
      activeTab: '1',
      authorities: props.authorities,
      displayAlert: false,
      displaySwagger: false,
      expanded: false,
      isAuthenticated: props.isAuthenticated,
      search: null,
      activePage: 1,
      totalPages: null,
      itemsCountPerPage: null,
      totalItemsCount: null,
      pageSize: 10
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handlePageChange = this.handlePageChange.bind(this);
    this.remove = this.remove.bind(this);
    this.toggle = this.toggle.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  toggle(tab) {
    if (this.state.activeTab !== tab) {
      this.setState({ activeTab: tab });
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    this.setState({search: value});
  }

  async handleSubmit(event) {
    event.preventDefault();
    await this.findAllTasks();
  }

  async componentDidMount() {
    if (this.props.match.params.page !== undefined) {
      this.setState({activePage: this.props.match.params.page});
    }
    toast.dismiss('Error');
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_READ' 
      || item === 'ROLE_TASK_READ' || item === 'ROLE_TASK_CREATE' 
      || item === 'ROLE_TASK_SAVE' || item === 'ROLE_TASK_DELETE' || item === 'SCOPE_openid')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        await this.findAllTasks();
      }
    }
  }

  async findAllTasks() {
    try {
      const { pageSize, activePage, search, jwt } = this.state;
      let url = `tasks?${search ? search : ''}${activePage ? '&page='+activePage : ''}${pageSize ? '&pageSize='+pageSize: ''}`;
      console.log("URL: {}", url);
      let data = await get(url, true, false, jwt);
      if (data) {
        if (Array.isArray(data.content)) {
          this.setState({ 
            isLoading: false, 
            tasks: data.content, 
            displaySwagger: true, 
            totalPages: data.totalPages, 
            itemsCountPerPage: data.size, 
            totalItemsCount: data.totalElements
          });
          this.props.history.push(`/${url}`);
        } else {
          if (data.status === 401) {
            this.setState({ displayAlert: true });
          }
          this.setState({ isLoading: false, displayError: errorMessage(data) });
        }
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    }
  }

  async handlePageChange(pageNumber) {
    console.log(`active page is ${pageNumber}`);
    this.setState({activePage: pageNumber})
    await this.findAllTasks();
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
    const { tasks, isLoading, displayError, authorities, displayAlert, displaySwagger, expanded } = this.state;

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_CREATE' || item === 'SCOPE_openid');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_SAVE' || item === 'SCOPE_openid');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_DELETE' || item === 'SCOPE_openid');

    const taskList = tasks.map(task => {
      return <tr key={task.id}>
        <th scope="row">{task.id}</th>
        <td style={{ whiteSpace: 'nowrap' }}>{task.name}</td>
        <td>{task.createdByUser}</td>
        <td>{task.createdDate}</td>
        <td>{task.lastModifiedByUser}</td>
        <td>{task.lastModifiedDate}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/tasks/" + task.id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({ 'id': task.id, 'name': task.name })} disabled={!hasDeleteAccess}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    const displayContent = () => {
      const { activePage, itemsCountPerPage, totalItemsCount } = this.state;
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
          <Nav tabs>
        <NavItem>
          <NavLink
            className={classnames({ active: this.state.activeTab === '1' })}
            onClick={() => { this.toggle('1'); }}>
            Tasks
        </NavLink>
        </NavItem>
        <NavItem>
          <NavLink
            className={classnames({ active: this.state.activeTab === '2' })}
            onClick={() => { this.toggle('2'); }}>
            Swagger UI
        </NavLink>
        </NavItem>
      </Nav>
      <TabContent activeTab={this.state.activeTab}>
        <TabPane tabId="1">
          <div className="float-right">
            <Button color="success" tag={Link} to="/tasks/new" disabled={!hasCreateAccess || displayAlert}>Add Task</Button>
          </div>
          <Form onSubmit={this.handleSubmit} inline>
            <FormGroup className="mb-2 mr-sm-2 mb-sm-0">
              <Label for="search" className="mr-sm-2">Search</Label>
              <Input 
                type="text" 
                name="search" 
                id="search" 
                onChange={this.handleChange} 
                placeholder="name=something" />
            </FormGroup>
            <Button>Search</Button>
          </Form>
          <div className="d-flex justify-content-center">
          <Pagination
              hideNavigation
              activePage={activePage}
              itemsCountPerPage={itemsCountPerPage}
              totalItemsCount={totalItemsCount}
              pageRangeDisplayed={10}
              itemClass='page-item'
              linkClass='btn btn-light'
              onChange={this.handlePageChange}
              />
          </div>
          <Table striped responsive>
            <thead>
              <tr>
                <th>#</th>
                <th>Name</th>
                <th>Created By</th>
                <th>Created Date</th>
                <th>Last Modified By User</th>
                <th>Last Modified By Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {taskList}
            </tbody>
          </Table>
          <div className="d-flex justify-content-center">
          <Pagination
              hideNavigation
              activePage={activePage}
              itemsCountPerPage={itemsCountPerPage}
              totalItemsCount={totalItemsCount}
              pageRangeDisplayed={10}
              itemClass='page-item'
              linkClass='btn btn-light'
              onChange={this.handlePageChange}
              />
            </div>
        </TabPane>
        <TabPane tabId="2">
          {/*this.state.activeTab === 2 ? <h3>Tab 2 Contents</h3> : null*/}
          {displaySwagger ?
          <Iframe url={`${taskSwaggerUrl}`}
            position="absolute"
            width="100%"
            id="myId"
            className="mt-4"
            height="100%" />
            : null}
        </TabPane>
      </TabContent>
      </div>
      }
    }

    return (
      <div
      style={{
        marginLeft: marginLeft(expanded),
        padding: '15px 20px 0 20px'
      }}
      >
        <AppNavbar />
        <Container fluid>
          <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
          {isLoading && <i class="fa fa-spinner" aria-hidden="true"></i>}
          {!isLoading && displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(TaskList);