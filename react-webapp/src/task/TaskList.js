import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
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
import PaginationComponent from "../common/Pagination";
import SearchButtonComponent from "../common/Search";
import { loading } from '../common/Loading';
import uuid from 'react-uuid';

const queryString = require('query-string');

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
      activePage: 0,
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

  setPageSize = (pageSize) => {
    this.setState({pageSize: pageSize});
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
    try {
      this.setLoading(true);
      event.preventDefault();
      await this.findAllTasks();
    } finally {
      this.setLoading(false);
    }
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      toast.dismiss('Error');
      let jwt = this.state.jwt;
      let permissions = this.state.authorities;
      if (jwt && permissions) {

        if (window.location.search !== "") {
          const parsed = queryString.parse(window.location.search);
          if (parsed.page !== undefined) {
            this.setState({activePage: parsed.page});
          }
      }

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_READ' 
        || item === 'ROLE_TASK_READ' || item === 'ROLE_TASK_CREATE' 
        || item === 'ROLE_TASK_SAVE' || item === 'ROLE_TASK_DELETE' || item === 'SCOPE_openid')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          await this.findAllTasks();
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  async findAllTasks(pageNumber) {
    try {
      const { pageSize, activePage, search, jwt } = this.state;
      let url = `tasks?${search ? search : ''}${pageNumber !== undefined ? '&page='+pageNumber : activePage ? '&page='+activePage : ''}${pageSize ? '&size='+pageSize: ''}`;
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
    try {
      this.setLoading(true);
      this.setState({activePage: pageNumber});
      await this.findAllTasks(pageNumber);
    } finally {
      this.setLoading(false);
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  async remove(task) {
    let confirm = await confirmDialog(`Delete Task ${task.name}`, "Are you sure you want to delete this?", "Delete Task");
    if (confirm) {
      let id = task.id;
      let jwt = this.state.jwt;
      await fetch(`/api/tasks/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt,
          'requestId': uuid()
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

    const suggestions = [
      {
        title: 'name',
        languages: [
          {
            name: 'C',
            year: 1972
          }
        ]
      },
      {
        title: 'createdByUser',
        languages: [
          {
            name: 'C++',
            year: 1983
          },
          {
            name: 'Perl',
            year: 1987
          }
        ]
      },
      {
        title: 'createdDate',
        languages: [
          {
            name: 'Haskell',
            year: 1990
          },
          {
            name: 'Python',
            year: 1991
          },
          {
            name: 'Java',
            year: 1995
          },
          {
            name: 'Javascript',
            year: 1995
          },
          {
            name: 'PHP',
            year: 1995
          },
          {
            name: 'Ruby',
            year: 1995
          }
        ]
      },
      {
        title: 'id',
        languages: [
          {
            name: 'C#',
            year: 2000
          },
          {
            name: 'Scala',
            year: 2003
          },
          {
            name: 'Clojure',
            year: 2007
          },
          {
            name: 'Go',
            year: 2009
          }
        ]
      },
      {
        title: '2010s',
        languages: [
          {
            name: 'Elm',
            year: 2012
          }
        ]
      }
    ];

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
          <div className="float-right"
                style={{
                  padding: '15px 20px 0 20px'
                }}
          >
            <Button color="success" tag={Link} to="/tasks/new" disabled={!hasCreateAccess || displayAlert}>Add Task</Button>
          </div>
          <SearchButtonComponent handleChange={this.handleChange} handleSubmit={this.handleSubmit} suggestions={suggestions} />
          <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
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
          <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
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
          {loading(isLoading)}
          {!isLoading && displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(TaskList);