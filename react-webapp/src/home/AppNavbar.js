import React, { Component } from 'react';
import { Collapse, Nav, Navbar, NavbarBrand, NavbarToggler, NavItem, NavLink } from 'reactstrap';
import { Link } from 'react-router-dom';

export default class AppNavbar extends Component {
  constructor(props) {
    super(props);
    this.state = { isOpen: false };
    this.toggle = this.toggle.bind(this);
  }

  toggle() {
    this.setState({
      isOpen: !this.state.isOpen
    });
  }

  render() {
    return <Navbar color="dark" dark expand="md">
      <NavbarBrand tag={Link} to="/">Home</NavbarBrand>
      <NavbarToggler onClick={this.toggle} />
      <Collapse isOpen={this.state.isOpen} navbar>
        <Nav className="ml-auto" navbar>
          <NavItem>
            <NavLink href="https://github.com/rodrigorodrigues/microservices-design-patterns">GitHub</NavLink>
          </NavItem>
          <NavItem>
            <NavLink disabled>Environment: {process.env.NODE_ENV}</NavLink>
          </NavItem>
          <NavItem>
            <NavLink disabled>URL API: {process.env.REACT_APP_GATEWAY_URL}</NavLink>
          </NavItem>
        </Nav>
      </Collapse>
    </Navbar>;
  }
}