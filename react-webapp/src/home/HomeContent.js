import React from 'react';
import { Container, Row, Col, UncontrolledDropdown, DropdownToggle, DropdownMenu, DropdownItem, Table, Jumbotron } from 'reactstrap';
import { Link } from 'react-router-dom';
import UserContext from '../UserContext';

function HomeContent({notDisplayMessage}) {
    const displayButton = (isAuthenticated, authorities) => {
        if (isAuthenticated) {
            return <Row>
                <Col xs="auto">
                    {displayButtonManagePeople(authorities)}
                </Col>
                <Col xs="auto">
                    {displayButtonManageUsers(authorities)}
                </Col>
                <Col xs="auto">
                    {displayButtonManageTasks(authorities)}
                </Col>
                <Col xs="auto">
                    {displayButtonManageCategories(authorities)}
                </Col>
                <Col xs="auto">
                    {displayButtonManageRecipes(authorities)}
                </Col>
                <Col xs="auto">
                    {displayButtonAdmin(authorities)}
                </Col>
                <Col xs="auto">
                    <Link className="link" to="/logout">Logout</Link>
                </Col>
            </Row>
        }
        return <Row>
            <Col xs="auto">
                <Link className="link" to="/login">Login</Link>
            </Col>
            </Row>
    }
    const displayMessage = (user) => {
        if (notDisplayMessage) {
            return "";
        }
        return user ?
            <h2>Welcome, {user}!</h2> :
            <p>Please log in to manage.</p>;
    }
    const displayUserPermissions = (user, jwt, authorities) => {
        if (notDisplayMessage || !user) {
            return "";
        }
        return (<div>
            <Jumbotron>
                <h1 className="display-3">User Details Permission</h1>
                <hr className="my-2" />
                <Table size="sm" borderless>
                    <tbody>
                        <tr>
                            <td><b>User</b></td>
                            <td>{user}</td>
                        </tr>
                        <tr>
                            <td><b>Authorities</b></td>
                            <td>{authorities}</td>
                        </tr>
                        <tr>
                            <td><b>JWT</b></td>
                            <td>{jwt}</td>
                        </tr>
                    </tbody>
                </Table>
            </Jumbotron>
        </div>
        )
    }
    return <UserContext.Consumer >
        {({user, isAuthenticated, authorities, jwt}) => <Container fluid>
            {displayMessage(user)}
            {displayButton(isAuthenticated, authorities)}
            {displayUserPermissions(user, jwt, authorities)}
        </Container>}
    </UserContext.Consumer> 
}

function displayButtonManagePeople(authorities) {
    const hasManageReadAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_READ' 
    || item === 'ROLE_PERSON_CREATE' || item === 'ROLE_PERSON_SAVE' || item === 'ROLE_PERSON_DELETE')
    return <Link to="/persons" className={"link" + (!hasManageReadAccess ? " disabled-link" : "")}>Manage People</Link>
}

function displayButtonManageUsers(authorities) {
    const isAdmin = authorities.some(item => item === 'ROLE_ADMIN')
    return <Link to="/users" className={"link" + (!isAdmin ? " disabled-link" : "")}>Manage Users</Link>
}

function displayButtonAdmin(authorities) {
    const isAdmin = authorities.some(item => item === 'ROLE_ADMIN')
    return (
        <UncontrolledDropdown>
                <DropdownToggle nav caret>
                    Admin
                </DropdownToggle>
                <DropdownMenu>
                <DropdownItem>
                    <Link to="/admin-eureka" className={"link" + (!isAdmin ? " disabled-link" : "")}>Eureka Server</Link>
                </DropdownItem>
                <DropdownItem>
                    <Link to="/admin-monitoring" className={"link" + (!isAdmin ? " disabled-link" : "")}>Spring Boot Admin</Link>
                </DropdownItem>
                <DropdownItem>
                    <Link to="/admin-grafana" className={"link" + (!isAdmin ? " disabled-link" : "")}>Grafana</Link>
                </DropdownItem>
                <DropdownItem>
                    <Link to="/admin-tracing" className={"link" + (!isAdmin ? " disabled-link" : "")}>Zipkin</Link>
                </DropdownItem>
            </DropdownMenu>
            </UncontrolledDropdown>
    )
}

function displayButtonManageCategories(authorities) {
    const hasManageReadAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_CATEGORY_READ' 
    || item === 'ROLE_CATEGORY_CREATE' || item === 'ROLE_CATEGORY_SAVE' || item === 'ROLE_CATEGORY_DELETE')
    return <Link to="/categories" className={"link" + (!hasManageReadAccess ? " disabled-link" : "")}>Manage Categories</Link>
}

function displayButtonManageRecipes(authorities) {
    const hasManageReadAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_RECIPE_READ' 
    || item === 'ROLE_RECIPE_CREATE' || item === 'ROLE_RECIPE_SAVE' || item === 'ROLE_RECIPE_DELETE')
    return <Link className={"link" + (!hasManageReadAccess ? " disabled-link" : "")} to="/recipes">Manage Recipes</Link>
}

function displayButtonManageTasks(authorities) {
    const hasManageReadAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_READ' 
    || item === 'ROLE_TASK_CREATE' || item === 'ROLE_TASK_SAVE' || item === 'ROLE_TASK_DELETE')
    return <Link to="/tasks" className={"link" + (!hasManageReadAccess ? " disabled-link" : "")}>Manage Tasks</Link>
}

export default HomeContent