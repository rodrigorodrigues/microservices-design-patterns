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

const productSwaggerUrl = process.env.REACT_APP_PRODUCT_SWAGGER_URL;

class ProductList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      products: [], 
      isLoading: true, 
      jwt: props.jwt, 
      displayError: null,
      displayAlert: false,
      displaySwagger: false,
      activeTab: '1',
      authorities: props.authorities,
      expanded: false,
      isAuthenticated: props.isAuthenticated
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
      || item === 'ROLE_PRODUCT_READ' || item === 'ROLE_PRODUCT_CREATE' 
      || item === 'ROLE_PRODUCT_SAVE' || item === 'ROLE_PRODUCT_DELETE')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        try {
          let data = await get('products', true, false, jwt);
          if (data) {
            if (Array.isArray(data)) {
              this.setState({isLoading: false, products: data, displaySwagger: true});
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

  async remove(product) {
    let confirm = await confirmDialog(`Delete Product ${product.name}`, "Are you sure you want to delete this?", "Delete Product");
    if (confirm) {
      let id = product.id;
      let jwt = this.state.jwt;
      await fetch(`/api/products/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt
        },
        credentials: 'include'
      }).then((err) => {
        if (err.status !== 200) {
          this.setState({displayError: errorMessage(err)});
        } else {
          let products = [...this.state.products].filter(i => i._id.$oid !== id);
          this.setState({products: products});
        }
      });
    }
  }

  render() {
    const { products, isLoading, displayError, displayAlert, displaySwagger, expanded } = this.state;

    const productList = products.map(product => {
      return <tr key={product._id.$oid}>
        <th scope="row">{product._id.$oid}</th>
        <td style={{whiteSpace: 'nowrap'}}>{product.name}</td>
        <td>{product.quantity}</td>
        <td style={{whiteSpace: 'nowrap'}}>{product.category}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/products/" + product._id.$oid}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': product._id.$oid, 'name': product.name})}>Delete</Button>
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
              Products
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
              <Button color="success" tag={Link} to="/products/new">Add Product</Button>
            </div>

            <Table striped responsive>
              <thead>
              <tr>
                <th>#</th>
                <th>Name</th>
                <th>Category</th>
                <th>Quantities</th>
                <th>Actions</th>
              </tr>
              </thead>
              <tbody>
              {productList}
              </tbody>
            </Table>
          </TabPane>
          <TabPane tabId="2">
            {displaySwagger ?
              <Iframe url={`${productSwaggerUrl}`}
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

export default withRouter(ProductList);