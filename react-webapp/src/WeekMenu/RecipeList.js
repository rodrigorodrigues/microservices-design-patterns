import React, {Component} from 'react';
import {Button, ButtonGroup, Container, Table} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import {get} from "../services/ApiService";
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';

class RecipeList extends Component {
  constructor(props) {
    super(props);
    this.state = {recipes: [], isLoading: true, jwt: props.jwt, displayError: null};
    this.remove = this.remove.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    if (jwt) {
      try {
        let data = await get('week-menu/v2/recipe', true, false, jwt)
        if (data) {
          if (Array.isArray(data)) {
            this.setState({isLoading: false, recipes: data});
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
    const {recipes, isLoading, displayError} = this.state;

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

    return (
      <div>
        <AppNavbar/>
        <Container fluid>
          <div className="float-right">
            <Button color="success" tag={Link} to="/recipes/new">Add Recipe</Button>
          </div>
          <div className="float-left">
          <HomeContent notDisplayMessage={true}></HomeContent>
          </div>
          <div className="float-right">
          <h3>Recipes</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="30%">Name</th>
              <th width="62%">Categories</th>
            </tr>
            </thead>
            <tbody>
            {recipeList}
            </tbody>
          </Table>
          <MessageAlert {...displayError}></MessageAlert>
          </div>
        </Container>
      </div>
    );
  }
}

export default withRouter(RecipeList);