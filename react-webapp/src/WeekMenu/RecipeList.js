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

const recipeSwaggerUrl = process.env.REACT_APP_RECIPE_SWAGGER_URL;

class RecipeList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      recipes: [], 
      isLoading: true, 
      jwt: props.jwt, 
      displayError: null,
      displayAlert: false,
      displaySwagger: false,
      activeTab: '1',
      authorities: props.authorities
    };
    this.remove = this.remove.bind(this);
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

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_RECIPE_READ' 
      || item === 'ROLE_RECIPE_READ' || item === 'ROLE_RECIPE_CREATE' 
      || item === 'ROLE_RECIPE_SAVE' || item === 'ROLE_RECIPE_DELETE')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        try {
          let data = await get('week-menu/v2/recipe', true, false, jwt)
          if (data) {
            if (Array.isArray(data)) {
              this.setState({isLoading: false, recipes: data, displaySwagger: true});
            } else {
              this.setState({isLoading: false, displayError: errorMessage(data)});
            }
          }
        } catch (error) {
          console.log("Error!");
          this.setState({isLoading: false, displayError: errorMessage(error)});
        }
      }
    }
  }

  async remove(recipe) {
    let confirm = await confirmDialog(`Delete Recipe ${recipe.name}`, "Are you sure you want to delete this?", "Delete Recipe");
    if (confirm) {
      let id = recipe.id;
      let jwt = this.state.jwt;
      await fetch(`/api/week-menu/v2/recipe/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt
        },
        credentials: 'include'
      }).then((err) => {
        if (err.status !== 200) {
          this.setState({displayError: errorMessage(err)});
        } else {
          let recipes = [...this.state.recipes].filter(i => i._id !== id);
          this.setState({recipes: recipes});
        }
      });
    }
  }

  render() {
    const { recipes, isLoading, displayError, displayAlert, displaySwagger } = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const recipeList = recipes.map(recipe => {
      const categoryName = recipe.categories.map(category => category.name).join(", ");
      return <tr key={recipe._id}>
        <td style={{whiteSpace: 'nowrap'}}>{recipe.name}</td>
        <td style={{whiteSpace: 'nowrap'}}>{categoryName}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/recipes/" + recipe._id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': recipe._id, 'name': recipe.name})}>Delete</Button>
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
              Recipes
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
              <Button color="success" tag={Link} to="/recipes/new">Add Recipe</Button>
            </div>

            <Table striped responsive>
              <thead>
              <tr>
                <th>#</th>
                <th>Name</th>
                <th>Categories</th>
              </tr>
              </thead>
              <tbody>
              {recipeList}
              </tbody>
            </Table>
          </TabPane>
          <TabPane tabId="2">
            {displaySwagger ?
              <Iframe url={`${recipeSwaggerUrl}`}
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
      <div>
        <AppNavbar/>
        <Container fluid>
          <HomeContent notDisplayMessage={true}></HomeContent>
          {displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(RecipeList);