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

class IngredientList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      ingredients: [], 
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

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_INGREDIENT_READ' 
      || item === 'ROLE_INGREDIENT_READ' || item === 'ROLE_INGREDIENT_CREATE' 
      || item === 'ROLE_INGREDIENT_SAVE' || item === 'ROLE_INGREDIENT_DELETE' || item === 'SCOPE_openid')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        try {
          let data = await get('week-menu/ingredient', true, false, jwt);
          if (data) {
            if (Array.isArray(data)) {
              this.setState({isLoading: false, ingredients: data, displaySwagger: true});
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

  async remove(ingredient) {
    let confirm = await confirmDialog(`Delete Ingredient ${ingredient.name}`, "Are you sure you want to delete this?", "Delete Ingredient");
    if (confirm) {
      let id = ingredient.id;
      const { jwt, gatewayUrl } = this.state;
      await fetch(`${gatewayUrl}/api/week-menu/ingredient/${id}`, {
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
          let ingredients = [...this.state.ingredients].filter(i => i._id !== id);
          this.setState({ingredients: ingredients});
        }
      });
    }
  }

  render() {
    const { ingredients, isLoading, displayError, displayAlert, displaySwagger, expanded } = this.state;

    const ingredientList = ingredients.map(ingredient => {
      return <tr key={ingredient._id}>
        <th scope="row">{ingredient._id}</th>
        <td style={{whiteSpace: 'nowrap'}}>{ingredient.name}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/ingredients/" + ingredient._id} disabled>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': ingredient._id, 'name': ingredient.name})}>Delete</Button>
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
              Ingredients
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
              <Button color="success" tag={Link} to="/ingredients/new" disabled>Add Ingredient</Button>
            </div>

            <Table striped responsive>
              <thead>
              <tr>
                <th>#</th>
                <th>Name</th>
                <th>Actions</th>
              </tr>
              </thead>
              <tbody>
              {ingredientList}
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

export default withRouter(IngredientList);