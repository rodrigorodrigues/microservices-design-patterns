import React, {Component} from 'react';
import {Button, ButtonGroup, Container, Table} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import {Link, withRouter} from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import {get} from "../services/ApiService";
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';

class CategoryList extends Component {
  constructor(props) {
    super(props);
    this.state = {categories: [], isLoading: true, jwt: props.jwt, displayError: null};
    this.remove = this.remove.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    if (jwt) {
      try {
        let data = await get('week-menu/v2/category', true, false, jwt);
        if (data) {
          data = JSON.parse(data);
          if (Array.isArray(data)) {
            this.setState({isLoading: false, categories: data});
          } else {
            this.setState({isLoading: false, displayError: errorMessage(data)});
          }
        }
      } catch (error) {
        this.setState({ displayError: errorMessage(error)});
      }
    }
  }

  async remove(category) {
    let confirm = await confirmDialog(`Delete Category ${category.name}`, "Are you sure you want to delete this?", "Delete Category");
    if (confirm) {
      let id = category.id;
      let jwt = this.state.jwt;
      await fetch(`/api/week-menu/v2/categories/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt
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
    const {categories, isLoading, displayError} = this.state;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const categoryList = categories.map(category => {
      const productName = category.products.map(product => product.name).join(", ");
      const productQuantities = category.products.map(product => ({name: product.name, quantity: product.quantity}));
      return <tr key={category._id}>
        <td style={{whiteSpace: 'nowrap'}}>{category.name}</td>
        <td style={{whiteSpace: 'nowrap'}}>{productName}</td>
        <td>{productQuantities}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/categories/" + category._id}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({'id': category._id, 'name': category.name})}>Delete</Button>
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
          <div className="float-left">
          <HomeContent notDisplayMessage={true}></HomeContent>
          </div>
          <div className="float-right">
          <h3>Categories</h3>
          <Table className="mt-4">
            <thead>
            <tr>
              <th width="40%">Name</th>
              <th width="40%">Products</th>
              <th width="12%">Quantities</th>
            </tr>
            </thead>
            <tbody>
            {categoryList}
            </tbody>
          </Table>
          <MessageAlert {...displayError}></MessageAlert>
          </div>
        </Container>
      </div>
    );
  }
}

export default withRouter(CategoryList);