import React from 'react';
import { Button, Container } from 'reactstrap';
import { Link } from 'react-router-dom';

function HomeContent({ user, isAuthenticated }) {
    const displayButton = () => {
        if (isAuthenticated) {
            return <div>
                <Button color="link">
                    <Link to="/persons">Manage JUG Tour</Link>
                </Button>
                <br />
                <Button color="link" onClick={this.logout}>Logout</Button>
            </div>
        }
        return <Button color="link">
            <Link to="/login">Login</Link>
        </Button>;
    }
    const displayMessage = () => {
        return user ?
            <h2>Welcome, {user.name}!</h2> :
            <p>Please log in to manage your JUG Tour.</p>;
    }
    return <Container fluid>
        {displayMessage()}
        {displayButton()}
    </Container>
}

export default HomeContent