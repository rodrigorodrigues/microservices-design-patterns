import React from 'react';
import { Button, Container } from 'reactstrap';
import { Link } from 'react-router-dom';
import UserContext from '../UserContext';

function HomeContent({logout}) {
    const displayButton = (isAuthenticated, authorities) => {
        if (isAuthenticated) {
            return <div>
                {displayButtonManagePeople(authorities)}
                <br />
                <Button color="link">
                    <Link to="/users">Manage Users</Link>
                </Button>
                <br />
                <Button color="link" onClick={logout}>Logout</Button>
            </div>
        }
        return <Button color="link">
            <Link to="/login">Login</Link>
        </Button>;
    }
    const displayMessage = (user) => {
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
    const isAdmin = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_READ')
    const myClass = isAdmin ? 'disabled' : ''
    return <Button color="link" className={myClass}>
        <Link to="/persons">Manage People</Link>
    </Button>
}

export default HomeContent
