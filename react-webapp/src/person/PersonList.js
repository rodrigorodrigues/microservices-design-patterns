import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import ChildModal from "./child/ChildModal";
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import HomeContent from '../home/HomeContent';
import FooterContent from '../home/FooterContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import Iframe from 'react-iframe';
import { toast } from 'react-toastify';
import { marginLeft } from '../common/Util';
import { get } from "../services/ApiService";
import PaginationComponent from "../common/Pagination";
import SearchButtonComponent from "../common/Search";
import { loading } from '../common/Loading';
import uuid from 'react-uuid';

const personSwaggerUrl = process.env.REACT_APP_PERSON_SWAGGER_URL;

class PersonList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      persons: [],
      isLoading: false,
      jwt: props.jwt,
      displayError: null,
      authorities: props.authorities,
      displayAlert: false,
      displaySwagger: false,
      activeTab: '1',
      isAuthenticated: props.isAuthenticated,
      user: props.user,
      expanded: false,
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
      await this.findAllPeople();
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
        if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_READ' 
        || item === 'ROLE_PERSON_READ' || item === 'ROLE_PERSON_CREATE' 
        || item === 'ROLE_PERSON_SAVE' || item === 'ROLE_PERSON_DELETE' || item === 'SCOPE_openid')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          await this.findAllPeople();
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  async findAllPeople(pageNumber) {
    try {
      const { pageSize, activePage, search, jwt } = this.state;
      let url = `people?${search ? search : ''}${pageNumber !== undefined ? '&page='+pageNumber : activePage ? '&page='+activePage : ''}${pageSize ? '&pageSize='+pageSize: ''}`;
      console.log(`URL: ${url}`);
      let data = await get(url, true, false, jwt);
      console.log("data: "+data);
      this.setState({ isLoading: false });
      if (data) {
        if (Array.isArray(data.content)) {
          this.setState({ 
            persons: data.content, 
            displaySwagger: true, 
            totalPages: data.totalPages, 
            itemsCountPerPage: data.size, 
            totalItemsCount: data.totalElements
          });
          this.props.history.push(`/${url}`);
        } else {
          if (data.status === 401) {
            this.setState({ displayAlert: true });
            this.props.onRemoveAuthentication();
          }
          this.setState({ displayError: errorMessage(data) });
        }
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    }
  }

  async handlePageChange(pageNumber) {
    try {
      this.setLoading(true);
      this.setState({activePage: pageNumber})
      await this.findAllPeople(pageNumber);
    } finally {
      this.setLoading(false);
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  async remove(person) {
    let confirm = await confirmDialog(`Delete Person ${person.name}`, "Are you sure you want to delete this?", "Delete Person");
    if (confirm) {
      let id = person.id;
      let jwt = this.state.jwt;
      await fetch(`/api/people/${id}`, {
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
          let persons = [...this.state.persons].filter(i => i.id !== id);
          this.setState({ persons: persons });
        }
      });
    }
  }

  render() {
    const { persons, isLoading, displayError, authorities, displayAlert, displaySwagger, expanded } = this.state;
    console.log("render:isLoading: "+isLoading);

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_CREATE' || item === 'SCOPE_openid');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_SAVE' || item === 'SCOPE_openid');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_DELETE' || item === 'SCOPE_openid');

    const personList = persons.map(person => {
      const address = `${person.address.address || ''} ${person.address.city || ''} ${person.address.stateOrProvince || ''}`;
      return <tr key={person.id}>
        <th scope="row">{person.id}</th>
        <td style={{ whiteSpace: 'nowrap' }}>{person.fullName}</td>
        <td>{person.createdByUser}</td>
        <td>{person.createdDate}</td>
        <td>{person.lastModifiedByUser}</td>
        <td>{person.lastModifiedDate}</td>
        <td>{person.dateOfBirth}</td>
        <td>{person.children ? <ChildModal person={person} /> : 'No Child'}</td>
        <td>{address}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/people/" + person.id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({ 'id': person.id, 'name': person.fullName })} disabled={!hasDeleteAccess}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    const displayContent = () => {
      const suggestions = [
        {
          title: 'name',
          languages: [
            {
              name: 'C',
              year: 1972
            }
          ]
        }
      ];
  
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
                Persons
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
                <Button color="success" tag={Link} to="/people/new" disabled={!hasCreateAccess || displayAlert}>Add Person</Button>
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
                    <th>Date of Birth</th>
                    <th>Children List</th>
                    <th>Location</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {personList}
                </tbody>
              </Table>
              <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
            </TabPane>
            <TabPane tabId="2">
              {displaySwagger ?
                <Iframe url={`${personSwaggerUrl}`}
                  position="relative"
                  width="100%"
                  id="myId"
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

export default withRouter(PersonList);