import React, {Component} from 'react';
import {Button, ButtonGroup, Container, Table} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import {get} from "../services/ApiService";

class RecipeList extends Component {
  constructor(props) {
    super(props);
    this.state = {recipes: [], isLoading: true, jwt: props.jwt, displayError: null};
    this.remove = this.remove.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    console.log("JWT: ", jwt);
    if (jwt) {
      try {
        const data = await get('week-menu/v2/recipe', true, false, jwt)
        if (data) {
          let jsonData = JSON.parse(data);
          this.setState({isLoading: false, recipes: jsonData});
        } else {
          this.setState({ displayError: errorMessage(data)});
        }
      } catch (error) {
        if (error.status === 401 || error.status === 403) {
          this.props.onRemoveAuthentication();
          this.props.history.push('/');
        }
        this.setState({ displayError: errorMessage(error)});
      }
    }
  }

  async remove(id) {
    await fetch(`/api/week-menu/v2/recipe/${id}`, {
      method: 'DELETE',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      credentials: 'include'
    }).then(() => {
      let recipes = [...this.state.recipes].filter(i => i._id !== id);
      this.setState({recipes: recipes});
    });
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
            <Button size="sm" color="danger" onClick={() => this.remove(recipe._id)}>Delete</Button>
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
          <h3>Recipes</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="30%">Name</th>
              <th width="60%">Categories</th>
            </tr>
            </thead>
            <tbody>
            {recipeList}
            </tbody>
          </Table>
          <MessageAlert {...displayError}></MessageAlert>
        </Container>
      </div>
    );
  }
}

export default withRouter(RecipeList);