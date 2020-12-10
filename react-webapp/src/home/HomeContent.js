import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import styled from 'styled-components';
import SideNav, { Toggle, Nav, NavItem, NavIcon, NavText } from './StyledSideNav';
import ClickOutside from 'react-click-outside';
import Breadcrumbs from '@trendmicro/react-breadcrumbs';
import Anchor from '@trendmicro/react-anchor';
import { marginLeft } from '../common/Util';
import { toast } from 'react-toastify';

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
    margin-left: ${props => marginLeft(props.expanded)};
`;

class HomeContent extends Component {
    constructor(props) {
        super(props);
        this.state = {
            selected: 'home',
            expanded: false,
            authorities: props.authorities,
            isAuthenticated: props.isAuthenticated,
            user: props.user,
            jwt: props.jwt
        };
    }

    componentDidMount() {
        toast.dismiss('Error');
        let jwt = this.state.jwt;
        console.log("HomeContent:componentDidMount:jwt: "+jwt);
    }

    onSelect = (selected) => {
        console.log("Selected: " + selected);        
        this.setState({ selected: selected });
    };

    onToggle = (expanded) => {
        this.setState({ expanded: expanded });
        const { setExpanded } = this.props;
        setExpanded(expanded);
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

    displayTasksButton(authorities) {
        const hasTaskPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_READ' 
        || item === 'ROLE_TASK_CREATE' || item === 'ROLE_TASK_SAVE' || item === 'ROLE_TASK_DELETE'));
        console.log("hasTaskPermission: "+hasTaskPermission);
        return (<NavItem eventKey="tasks" disabled={(!hasTaskPermission)}>
            <NavIcon>
                <i className="fa fa-fw fa-tasks" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Tasks">
                Tasks
            </NavText>
        </NavItem>);
    }

    displayPostsButton(authorities) {
        const hasPostPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_POST_READ' 
        || item === 'ROLE_POST_CREATE' || item === 'ROLE_POST_SAVE' || item === 'ROLE_POST_DELETE'));
        console.log("hasTaskPermission: "+hasPostPermission);
        return (<NavItem eventKey="posts" disabled={(!hasPostPermission)}>
            <NavIcon>
                <i className="fa fa-fw fa-comments" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Posts">
                Posts
            </NavText>
        </NavItem>);
    }

    displayProductsButton(authorities) {
        const hasProductPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PRODUCT_READ' 
        || item === 'ROLE_PRODUCT_CREATE' || item === 'ROLE_PRODUCT_SAVE' || item === 'ROLE_PRODUCT_DELETE'));
        console.log("hasTaskPermission: "+hasProductPermission);
        return (<NavItem eventKey="products" disabled={(!hasProductPermission)}>
            <NavIcon>
                <i className="fa fa-fw fa-truck" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Products">
                Products
            </NavText>
        </NavItem>);
    }

    displayAdminButtons(authorities) {
        const isAdmin = this.hasAdminAccess(authorities);
        console.log("isAdmin: "+isAdmin);
        return (<NavItem eventKey="admin" disabled={(!isAdmin)}>
            <NavIcon>
                <i className="fa fa-fw fa-cogs" style={{ fontSize: '1.5em' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Admin">
                Admin
            </NavText>
            <NavItem eventKey="users" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-users" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Users">
                    Users
                </NavText>
            </NavItem>
            <NavItem eventKey="consul" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-codepen" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Consul - Discovery Server">
                    Consul
                </NavText>
            </NavItem>
            <NavItem eventKey="grafana" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-pie-chart" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Grafana">
                    Grafana
                </NavText>
            </NavItem>
            <NavItem eventKey="prometheus" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-free-code-camp" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Prometheus">
                    Prometheus
                </NavText>
            </NavItem>
            <NavItem eventKey="jaeger" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-bug" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Jaeger">
                    Jaeger
                </NavText>
            </NavItem>
        </NavItem>);
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

    renderBreadcrumbs() {
        const { isAuthenticated, authorities } = this.props;
        return (
            <Breadcrumbs showLineSeparator>
                <Breadcrumbs.Item>
                    <Anchor href="/home">Home</Anchor>
                </Breadcrumbs.Item>
                {!isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="/login">Login</Anchor>
                </Breadcrumbs.Item>}
                {this.hasAdminAccess(authorities) &&
                <Breadcrumbs.Item>
                    <Anchor href="/users">Manage Users</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="/people">Manage People</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="/tasks">Manage Tasks</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="/posts">Manage Posts</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="/products">Manage Products</Anchor>
                </Breadcrumbs.Item>}
                {this.hasAdminAccess(authorities) &&
                <Breadcrumbs.Item>
                    <Anchor href="/admin">Admin Access</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
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
                <div>
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
                        this.onSelect(selected);
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
                        <div>User: <b>{user}</b></div>
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
                        {this.displayLoginButton()}
                        {this.displayAdminButtons(authorities)}
                        {this.displayPeopleButton(authorities)}
                        {this.displayTasksButton(authorities)}
                        {this.displayPostsButton(authorities)}
                        {this.displayProductsButton(authorities)}
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