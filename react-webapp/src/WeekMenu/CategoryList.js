import React, {Component} from 'react';
import {Button, ButtonGroup, Container, Table} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import {get} from "../services/ApiService";

class CategoryList extends Component {
  constructor(props) {
    super(props);
    this.state = {categories: [], isLoading: true, jwt: props.jwt, displayError: null};
    this.remove = this.remove.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    console.log("JWT: ", jwt);
    if (jwt) {
      try {
        const data = await get('week-menu/v2/category', true, false,
            {'Authorization': jwt})
        if (data) {
          this.state.categories.push(data);
          this.setState({isLoading: false, categories: this.state.categories});
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
    await fetch(`/api/week-menu/v2/categories/${id}`, {
      method: 'DELETE',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      credentials: 'include'
    }).then(() => {
      let categories = [...this.state.categories].filter(i => i.id !== id);
      this.setState({categories: categories});
    });
  }

  render() {
    const {categories, isLoading, displayError} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const categoryList = categories.map(category => {
      return <tr key={category.id}>
        <td style={{whiteSpace: 'nowrap'}}>{category.email}</td>
        <td>{category.fullName}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/categories/" + category.id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove(category.id)}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    return (
      <div>
        <AppNavbar/>
        <Container fluid>
          <div className="float-right">
            <Button color="success" tag={Link} to="/categories/new">Add Category</Button>
          </div>
          <h3>Categories</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="20%">Email</th>
              <th width="80%">Full Name</th>
            </tr>
            </thead>
            <tbody>
            {categoryList}
            </tbody>
          </Table>
          <MessageAlert {...displayError}></MessageAlert>
        </Container>
      </div>
    );
  }
}

export default withRouter(CategoryList);