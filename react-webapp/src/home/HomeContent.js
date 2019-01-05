import React from 'react';
import { Button, Container } from 'reactstrap';
import { Link } from 'react-router-dom';
import UserContext from '../UserContext';

function HomeContent({logout}) {
    const displayButton = (isAuthenticated) => {
        if (isAuthenticated) {
            return <div>
                <Button color="link">
                    <Link to="/persons">Manage JUG Tour</Link>
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
            <h2>Welcome, {user.name}!</h2> :
            <p>Please log in to manage your JUG Tour.</p>;
    }
    return <UserContext.Consumer >
        {({user, isAuthenticated}) => <Container fluid>
            {displayMessage(user)}
            {displayButton(isAuthenticated)}
        </Container>}
    </UserContext.Consumer> 
        
        
            
}

export default HomeContent
