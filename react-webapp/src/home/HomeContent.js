import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import styled from 'styled-components';
import SideNav, { Toggle, Nav, NavItem, NavIcon, NavText } from './StyledSideNav';
import ClickOutside from 'react-click-outside';
import Breadcrumbs from '@trendmicro/react-breadcrumbs';
import useGlobal from "../common/useGlobal";
import Anchor from '@trendmicro/react-anchor';

const navWidthCollapsed = 64;
const navWidthExpanded = 280;

const NavHeader = styled.div`
    display: ${props => (props.expanded ? 'block' : 'none')};
    white-space: nowrap;
    background-color: #db3d44;
    color: #fff;
    > * {
        color: inherit;
        background-color: inherit;
    }
`;

// height: 20px + 10px + 10px = 40px
const NavTitle = styled.div`
    font-size: 2em;
    line-height: 20px;
    padding: 10px 0;
`;

// height: 20px + 4px = 24px;
const NavSubTitle = styled.div`
    font-size: 1em;
    line-height: 20px;
    padding-bottom: 4px;
`;

const NavInfoPane = styled.div`
    float: left;
    width: 100%;
    padding: 10px 20px;
    background-color: #eee;
`;

const Separator = styled.div`
    clear: both;
    position: relative;
    margin: .8rem 0;
    background-color: #ddd;
    height: 1px;
`;

const Main = styled.main`
    position: relative;
    overflow: hidden;
    transition: all .15s;
    padding: 0 20px;
    margin-left: ${props => (props.expanded ? 240 : 64)}px;
`;

class HomeContent extends Component {
    constructor(props) {
        super(props);
        this.state = {
            selected: 'home',
            expanded: false,
            authorities: props.authorities,
            isAuthenticated: props.isAuthenticated,
            user: props.user
        };
    }

    lastUpdateTime = new Date().toISOString();

    onSelect = (selected) => {
        console.log("Selected: " + selected);        
        this.setState({ selected: selected });
    };

    onToggle = (expanded) => {
        this.setState({ expanded: expanded });
        console.log("onToggle:expanded: "+expanded);
    };

    displayPeopleButton(authorities) {
        console.log("authorities: "+authorities);
        const hasManageReadAccess = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_READ' 
        || item === 'ROLE_PERSON_CREATE' || item === 'ROLE_PERSON_SAVE' || item === 'ROLE_PERSON_DELETE'));
        console.log("hasManageReadAccess: "+hasManageReadAccess);
        return (<NavItem eventKey="people" disabled={(!hasManageReadAccess)}>
            <NavIcon>
                <i className="fa fa-fw fa-user" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="People">
                People
            </NavText>
        </NavItem>);
    }

    displayAdminButtons(authorities) {
        const isAdmin = this.hasAdminAccess(authorities);
        console.log("isAdmin: "+isAdmin);
        if (isAdmin) {
            return (<NavItem eventKey="admin">
            <NavIcon>
                <i className="fa fa-fw fa-cogs" style={{ fontSize: '1.5em' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Admin">
                Admin
            </NavText>
            <NavItem eventKey="users">
                <NavIcon>
                    <i className="fa fa-fw fa-users" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Users">
                    Users
                </NavText>
            </NavItem>
            <NavItem eventKey="settings/network">
                <NavText title="NETWORK">
                    NETWORK
                </NavText>
            </NavItem>
        </NavItem>);
        } else {
            return null;
        }
    }

    hasAdminAccess(authorities) {
        return ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN'));
    }

    displayLogoutButton() {
        console.log("isAuthenticated: "+this.state.isAuthenticated);
        if (this.state.isAuthenticated) {
            return (<NavItem eventKey="logout">
            <NavIcon>
                <i className="fa fa-fw fa-power-off" style={{ fontSize: '1.5em' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Sign-out">
                Sign-out
            </NavText>
        </NavItem>);
        } else {
            return null;
        }
    }

    displayLoginButton() {
        if (!this.state.isAuthenticated) {
            return (                        <NavItem eventKey="login">
            <NavIcon>
                <i className="fa fa-fw fa-sign-in" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Sign-in">
                Sign-in
            </NavText>
            </NavItem>);
        } else {
            return null;
        }
    }

    shareExpanded = (expanded) => {//TODO Need to figure out how to call this
        const [globalState, globalActions] = useGlobal();
        console.log("shareExpanded:globalState: "+globalState);
        globalActions.shareExpanded(expanded);
        return null;
      };
      

    renderBreadcrumbs() {
        return (
            <Breadcrumbs showLineSeparator>
                <Breadcrumbs.Item>
                    <Anchor href="/home">Home</Anchor>
                </Breadcrumbs.Item>
                {!this.state.isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="/login">Login</Anchor>
                </Breadcrumbs.Item>}
                {this.hasAdminAccess(this.state.authorities) &&
                <Breadcrumbs.Item>
                    <Anchor href="/users">Manage Users</Anchor>
                </Breadcrumbs.Item>}
                {this.state.isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="/people">Manage People</Anchor>
                </Breadcrumbs.Item>}
                {this.hasAdminAccess(this.state.authorities) &&
                <Breadcrumbs.Item>
                    <Anchor href="/admin">Admin Access</Anchor>
                </Breadcrumbs.Item>}
                {this.state.isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="/logout">Logout</Anchor>
                </Breadcrumbs.Item>}
            </Breadcrumbs>            
        );
    }

    render() {
        const { expanded, selected, authorities, user } = this.state;

        return (
            <div>
                <div
                    style={{
                        marginLeft: expanded ? 240 : 64,
                        padding: '15px 20px 0 20px'
                    }}
                >
                <ClickOutside
                    onClickOutside={() => {
                        this.onToggle(false);
                    }}
                >
                <SideNav
                    expanded={this.state.expanded}
                    style={{ minWidth: expanded ? navWidthExpanded : navWidthCollapsed }}
                    onToggle={this.onToggle}
                    onSelect={(selected) => {
                        console.log("Selected App: " + selected);
                        console.log("This properties: " + this.props);
                        const to = '/' + selected;
                        if (this.props.location.pathname !== to) {
                            this.props.history.push(to);
                        }
                    }}
                >

                    <Toggle />
                    <NavHeader expanded={expanded}>
                        <NavTitle>Menu</NavTitle>
                        <NavSubTitle>spendingbetter.com</NavSubTitle>
                    </NavHeader>
                    {expanded && user &&
                    <NavInfoPane>
                        <div>Time: {this.lastUpdateTime}</div>
                        <div>User: {user}</div>
                    </NavInfoPane>
                    }
                    <Nav
                        defaultSelected={selected}
                    >
                        <NavItem eventKey="home">
                            <NavIcon>
                                <i className="fa fa-fw fa-home" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
                            </NavIcon>
                            <NavText style={{ paddingRight: 32 }} title="Home">
                                Home
                            </NavText>
                        </NavItem>
                        {this.displayAdminButtons(authorities)}
                        {this.displayLoginButton()}
                        {this.displayPeopleButton(authorities)}
                        <NavItem eventKey="reports">
                            <NavIcon>
                                <i className="fa fa-fw fa-list-alt" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
                            </NavIcon>
                            <NavText style={{ paddingRight: 32 }} title="REPORTS">
                                REPORTS
                            </NavText>
                        </NavItem>
                        <Separator />
                        {this.displayLogoutButton()}
                    </Nav>
                </SideNav>
                </ClickOutside>
                </div>
                <Main expanded={expanded}>
                    {this.renderBreadcrumbs()}
                </Main>
            </div>
        );
    }
}

export default withRouter(HomeContent);