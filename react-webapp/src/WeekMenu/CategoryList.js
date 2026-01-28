import React, {Component} from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import {get} from "../services/ApiService";
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import Iframe from 'react-iframe';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';
import { marginLeft } from '../common/Util';
import uuid from 'react-uuid';

const categorySwaggerUrl = process.env.REACT_APP_CATEGORY_SWAGGER_URL;

class CategoryList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      categories: [], 
      isLoading: true, 
      jwt: props.jwt, 
      displayError: null,
      displayAlert: false,
      displaySwagger: false,
      activeTab: '1',
      authorities: props.authorities,
      expanded: false,
      isAuthenticated: props.isAuthenticated,
      gatewayUrl: props.gatewayUrl
    };
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

  async componentDidMount() {
    toast.dismiss('Error');
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN'
      || item === 'ROLE_CATEGORY_READ' || item === 'ROLE_CATEGORY_CREATE' 
      || item === 'ROLE_CATEGORY_SAVE' || item === 'ROLE_CATEGORY_DELETE')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        try {
          let data = await get('week-menu/v2/category', true, false, jwt);
          if (data) {
            if (Array.isArray(data)) {
              this.setState({isLoading: false, categories: data, displaySwagger: true});
            } else {
              this.setState({isLoading: false, displayError: errorMessage(data)});
            }
          }
        } catch (error) {
          this.setState({ displayError: errorMessage(error)});
        }
      }
    }
  }

  async remove(category) {
    let confirm = await confirmDialog(`Delete Category ${category.name}`, "Are you sure you want to delete this?", "Delete Category");
    if (confirm) {
      let id = category.id;
      const { jwt, gatewayUrl } = this.state;
      await fetch(`${gatewayUrl}/api/week-menu/v2/category/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt,
          'requestId': uuid()
        },
        credentials: 'include'
      }).then((err) => {
        if (err.status !== 200) {
          this.setState({displayError: errorMessage(err)});
        } else {
          let categories = [...this.state.categories].filter(i => i._id !== id);
          this.setState({categories: categories});
        }
      });
    }
  }

  render() {
    const { categories, isLoading, displayError, authorities, displayAlert, displaySwagger, expanded } = this.state;

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_CATEGORY_CREATE' || item === 'SCOPE_openid');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_CATEGORY_SAVE' || item === 'SCOPE_openid');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_CATEGORY_DELETE' || item === 'SCOPE_openid');

    const categoryList = categories.map(category => {
      const productName = category.products.map(product => product.name).join(", ");
      const productQuantities = category.products.map(product => ({name: product.name, quantity: product.quantity}));
      return <tr key={category._id}>
        <th scope="row">{category._id}</th>
        <td style={{whiteSpace: 'nowrap'}}>{category.name}</td>
        <td style={{whiteSpace: 'nowrap'}}>{productName}</td>
        <td>{productQuantities}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/categories/" + category._id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': category._id, 'name': category.name})} disabled={!hasDeleteAccess}>Delete</Button>
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
              Categories
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
              <Button color="success" tag={Link} to="/categories/new" disabled={!hasCreateAccess || displayAlert}>Add Category</Button>
            </div>

            <Table striped responsive>
              <thead>
              <tr>
                <th>#</th>
                <th>Name</th>
                <th>Products</th>
                <th>Quantities</th>
                <th>Actions</th>
              </tr>
              </thead>
              <tbody>
              {categoryList}
              </tbody>
            </Table>
          </TabPane>
          <TabPane tabId="2">
            {displaySwagger ?
              <Iframe url={`${categorySwaggerUrl}`}
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
        <AppNavbar/>
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

export default withRouter(CategoryList);