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
import Anchor from '@trendmicro/react-anchor';
import uuid from 'react-uuid';
const moment = require('moment');

const postSwaggerUrl = process.env.REACT_APP_POST_SWAGGER_URL;

class PostList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      posts: [],
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

  setPageSize = (pageSize) => {
    this.setState({pageSize: pageSize});
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    this.setState({search: value});
  }

  async handleSubmit(event) {
    try {
      this.setLoading(true);
      await this.findAllPosts();
    } finally {
      this.setLoading(false);
    }
  }

  toggle(tab) {
    if (this.state.activeTab !== tab) {
      this.setState({ activeTab: tab });
    }
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  async componentDidMount() {
    try {
      toast.dismiss('Error');
      let jwt = this.state.jwt;
      let permissions = this.state.authorities;
      if (jwt && permissions) {

        if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_POST_READ'
            || item === 'ROLE_POST_READ' || item === 'ROLE_POST_CREATE'
            || item === 'ROLE_POST_SAVE' || item === 'ROLE_POST_DELETE' || item === 'SCOPE_openid')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          await this.findAllPosts();
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  async findAllPosts(pageNumber) {
    try {
      const { pageSize, activePage, search, jwt } = this.state;
      let url = `posts?${search ? search : ''}${pageNumber !== undefined ? '&page='+pageNumber : activePage ? '&page='+activePage : ''}${pageSize ? '&size='+pageSize: ''}`;
      let data = await get(url, true, false, jwt);
      if (data) {
        if (Array.isArray(data.content)) {
          this.setState({
            isLoading: false,
            posts: data.content,
            displaySwagger: true,
            totalPages: data.totalPages,
            itemsCountPerPage: data.size,
            totalItemsCount: data.totalElements
          });
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
      await this.findAllPosts(pageNumber);
    } finally {
      this.setLoading(false);
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  displayTasks(post) {
    return (post.tasks !== undefined ?
        <div style={{ whiteSpace: 'nowrap' }}>
          {post.tasks.map(task =>
              <Anchor href={`/tasks/${task.id}`}>{task.name}</Anchor>
          )}
        </div>
        : '');
  }

  async remove(post) {
    let confirm = await confirmDialog(`Delete Post ${post.name}`, "Are you sure you want to delete this?", "Delete Post");
    if (confirm) {
      let id = post.id;
      let jwt = this.state.jwt;
      await fetch(`/api/posts/${id}`, {
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
          let posts = [...this.state.posts].filter(i => i.id !== id);
          this.setState({ posts: posts });
        }
      });
    }
  }

  render() {
    const { posts, isLoading, displayError, authorities, displayAlert, displaySwagger, expanded } = this.state;

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_POST_CREATE' || item === 'SCOPE_openid');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_POST_SAVE' || item === 'SCOPE_openid');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_POST_DELETE' || item === 'SCOPE_openid');

    const postList = posts.map(post => {
      return <tr key={post.id}>
        <th scope="row">{post.id}</th>
        <td style={{ whiteSpace: 'nowrap' }}>{post.name}</td>
        <td>{this.displayTasks(post)}</td>
        <td>{post.createdByUser}</td>
        <td>{moment(post.createdDate).format()}</td>
        <td>{post.lastModifiedByUser}</td>
        <td>{post.lastModifiedDate != null ? moment(post.lastModifiedDate).format(): ''}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/posts/" + post.id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({ 'id': post.id, 'name': post.name })} disabled={!hasDeleteAccess}>Delete</Button>
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
              name: 'C'
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
                Posts
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
                <Button color="success" tag={Link} to="/posts/new" disabled={!hasCreateAccess || displayAlert}>Add Post</Button>
              </div>
              <SearchButtonComponent handleChange={this.handleChange} handleSubmit={this.handleSubmit} suggestions={suggestions} />
              <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
              <Table striped responsive>
                <thead>
                <tr>
                  <th>#</th>
                  <th>Name</th>
                  <th>Tasks</th>
                  <th>Created By</th>
                  <th>Created Date</th>
                  <th>Last Modified By User</th>
                  <th>Last Modified By Date</th>
                  <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {postList}
                </tbody>
              </Table>
            </TabPane>
            <TabPane tabId="2">
              {/*this.state.activeTab === 2 ? <h3>Tab 2 Contents</h3> : null*/}
              {displaySwagger ?
                  <Iframe url={`${postSwaggerUrl}`}
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

export default withRouter(PostList);