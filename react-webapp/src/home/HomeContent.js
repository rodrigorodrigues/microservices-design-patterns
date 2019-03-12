import React from 'react';
import { Button, Container } from 'reactstrap';
import { Link } from 'react-router-dom';
import UserContext from '../UserContext';

function HomeContent({logout, notDisplayMessage}) {
    const displayButton = (isAuthenticated, authorities) => {
        if (isAuthenticated) {
            return <div>
                {displayButtonManagePeople(authorities)}
                <br />
                {displayButtonManageUsers(authorities)}
                <br />
                {displayButtonManageCategories(authorities)}
                <br />
                {displayButtonManageRecipes(authorities)}
                <br />
                <Button color="link" onClick={logout}>Logout</Button>
            </div>
        }
        return <Button color="link">
            <Link to="/login">Login</Link>
        </Button>;
    }
    const displayMessage = (user) => {
        if (notDisplayMessage) {
            return "";
        }
        return user ?
            <h2>Welcome, {user}!</h2> :
            <p>Please log in to manage.</p>;
    }
    return <UserContext.Consumer >
        {({user, isAuthenticated, authorities}) => <Container fluid>
            {displayMessage(user)}
            {displayButton(isAuthenticated, authorities)}
        </Container>}
    </UserContext.Consumer> 
}
function displayButtonManagePeople(authorities) {
    const hasManageReadAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_READ' 
    || item === 'ROLE_PERSON_CREATE' || item === 'ROLE_PERSON_SAVE' || item === 'ROLE_PERSON_DELETE')
    return <Button color="link" disabled={!hasManageReadAccess}>
        <Link to="/persons">Manage People</Link>
    </Button>
}

function displayButtonManageUsers(authorities) {
    const isAdmin = authorities.some(item => item === 'ROLE_ADMIN')
    return <Button color="link" disabled={!isAdmin}>
    <Link to="/users">Manage Users</Link>
</Button>

}

function displayButtonManageCategories(authorities) {
    const isAdmin = authorities.some(item => item === 'ROLE_ADMIN')
    return <Button color="link" disabled={!isAdmin}>
    <Link to="/categories">Manage Categories</Link>
    </Button>
}

function displayButtonManageRecipes(authorities) {
    const isAdmin = authorities.some(item => item === 'ROLE_ADMIN')
    return <Button color="link" disabled={!isAdmin}>
    <Link to="/recipes">Manage Recipes</Link>
    </Button>
}

export default HomeContent
