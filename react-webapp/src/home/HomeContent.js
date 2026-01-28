import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import styled from 'styled-components';
import SideNav, { Toggle, Nav, NavItem, NavIcon, NavText } from './StyledSideNav';
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
        const hasManageReadAccess = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PERSON_READ' 
        || item === 'ROLE_PERSON_CREATE' || item === 'ROLE_PERSON_SAVE' || item === 'ROLE_PERSON_DELETE' || item === 'SCOPE_openid'));
        return (<NavItem eventKey="people" disabled={(!hasManageReadAccess)}>
            <NavIcon>
                <i className="fa fa-fw fa-user" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="java service - people" />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="People">
                People
            </NavText>
        </NavItem>);
    }

    displayTasksButton(authorities) {
        const hasTaskPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_TASK_READ' 
        || item === 'ROLE_TASK_CREATE' || item === 'ROLE_TASK_SAVE' || item === 'ROLE_TASK_DELETE' || item === 'SCOPE_openid'));
        return (<NavItem eventKey="tasks" disabled={(!hasTaskPermission)}>
            <NavIcon>
                <i className="fa fa-fw fa-tasks" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="kotlin service - tasks" />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Tasks">
                Tasks
            </NavText>
        </NavItem>);
    }

    displayPostsButton(authorities) {
        const hasPostPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_POST_READ' 
        || item === 'ROLE_POST_CREATE' || item === 'ROLE_POST_SAVE' || item === 'ROLE_POST_DELETE' || item === 'SCOPE_openid'));
        return (<NavItem eventKey="posts" disabled={(!hasPostPermission)}>
            <NavIcon>
                <i className="fa fa-fw fa-comments" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="golang service - posts" />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Posts">
                Posts
            </NavText>
        </NavItem>);
    }

    displayProductsButton(authorities) {
        const hasProductPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_PRODUCT_READ' 
        || item === 'ROLE_PRODUCT_CREATE' || item === 'ROLE_PRODUCT_SAVE' || item === 'ROLE_PRODUCT_DELETE' || item === 'SCOPE_openid'));
        return (<NavItem eventKey="products" disabled={(!hasProductPermission)}>
            <NavIcon>
                <i className="fa fa-fw fa-truck" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="python service - products" />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Products">
                Products
            </NavText>
        </NavItem>);
    }

    displayAdminButtons(authorities) {
        const isAdmin = this.hasAdminAccess(authorities);
        return (<NavItem eventKey="admin" disabled={(!isAdmin)}>
            <NavIcon>
                <i className="fa fa-fw fa-cogs" style={{ fontSize: '1.5em' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Admin">
                Admin
            </NavText>
            <NavItem eventKey="users" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-users" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Admin - List of Users" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Users">
                    Users
                </NavText>
            </NavItem>
            <NavItem eventKey="consul" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-codepen" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Service Discovery - Consul" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Consul - Discovery Server">
                    Consul
                </NavText>
            </NavItem>
            <NavItem eventKey="monitoring" disabled={(!isAdmin)}>
                <NavIcon title="Monitoring - Spring Boot Admin">
                    <i className="fa fa-fw fa-exclamation-triangle" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Monitoring" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Monitoring - Spring Boot Admin">
                    Monitoring
                </NavText>
            </NavItem>
            <NavItem eventKey="grafana" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-pie-chart" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Dashboard - Grafana" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Grafana">
                    Grafana
                </NavText>
            </NavItem>
            <NavItem eventKey="prometheus" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-free-code-camp" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Metrics - Prometheus" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Prometheus">
                    Prometheus
                </NavText>
            </NavItem>
            <NavItem eventKey="jaeger" disabled={(!isAdmin)}>
                <NavIcon>
                    <i className="fa fa-fw fa-bug" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Tracing - Jaeger" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Jaeger">
                    Jaeger
                </NavText>
            </NavItem>
        </NavItem>);
    }

    displayWeekMenuButtons(authorities) {
        const hasCategoryPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_CATEGORY_READ' 
        || item === 'ROLE_CATEGORY_CREATE' || item === 'ROLE_CATEGORY_SAVE' || item === 'ROLE_CATEGORY_DELETE' || item === 'SCOPE_openid'));
        const hasRecipePermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_RECIPE_READ' 
        || item === 'ROLE_RECIPE_CREATE' || item === 'ROLE_RECIPE_SAVE' || item === 'ROLE_RECIPE_DELETE' || item === 'SCOPE_openid'));
        const hasIngredientPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_INGREDIENT_READ' 
        || item === 'ROLE_INGREDIENT_CREATE' || item === 'ROLE_INGREDIENT_SAVE' || item === 'ROLE_INGREDIENT_DELETE' || item === 'SCOPE_openid'));
        return (<NavItem eventKey="weekMenu" disabled={(authorities === undefined)}>
            <NavIcon>
                <i className="fa fa-fw fa-wrench" style={{ fontSize: '1.5em' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="weekMenu">
                Week Menu
            </NavText>
            <NavItem eventKey="categories" disabled={(!hasCategoryPermission)}>
                <NavIcon>
                    <i className="fa fa-fw fa-cog" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Manage Categories" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="categories">
                    Manage Categories
                </NavText>
            </NavItem>
            <NavItem eventKey="recipes" disabled={(!hasRecipePermission)}>
                <NavIcon>
                    <i className="fa fa-fw fa-codepen" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Manage Receipts" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="recipes">
                    Manage Recipes
                </NavText>
            </NavItem>
            <NavItem eventKey="ingredients" disabled={(!hasIngredientPermission)}>
                <NavIcon title="Manage Ingredients">
                    <i className="fa fa-fw fa-paint-brush" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Manage Ingredients" />
                </NavIcon>
                <NavText style={{ paddingRight: 32 }} title="Manage Ingredients">
                    Manage Ingredients
                </NavText>
            </NavItem>
        </NavItem>);
    }

    hasAdminAccess(authorities) {
        return ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN'));
    }

    displayLogoutButton() {
        if (this.state.isAuthenticated) {
            return (<NavItem eventKey="logout">
            <NavIcon>
                <i className="fa fa-fw fa-power-off" style={{ fontSize: '1.5em' }} title="Sign-out" />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Sign-out">
                Sign-out
            </NavText>
        </NavItem>);
        } else {
            return null;
        }
    }

    displayPasskeyButton() {
        if (this.state.isAuthenticated) {
            return (<NavItem eventKey="passkeys">
            <NavIcon>
                <i className="fa fa-fw fa-user-secret" style={{ fontSize: '1.5em' }} title="Passkeys" />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Passkeys">
                Passkeys
            </NavText>
        </NavItem>);
        } else {
            return null;
        }
    }

    displayLoginButton() {
        if (!this.state.isAuthenticated) {
            return (<NavItem eventKey="login">
            <NavIcon>
                <i className="fa fa-fw fa-sign-in" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} title="Sign-in" />
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
                    <Anchor href="./home">Home</Anchor>
                </Breadcrumbs.Item>
                {!isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="./login">Login</Anchor>
                </Breadcrumbs.Item>}
                {this.hasAdminAccess(authorities) &&
                <Breadcrumbs.Item>
                    <Anchor href="./users">Manage Users</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="./people">Manage People</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="./tasks">Manage Tasks</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="./companies">Manage Companies</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="./posts">Manage Posts</Anchor>
                </Breadcrumbs.Item>}
                {this.hasAdminAccess(authorities) &&
                <Breadcrumbs.Item>
                    <Anchor href="./admin/createAll">Admin - Create All</Anchor>
                </Breadcrumbs.Item>}
                {isAuthenticated &&
                <Breadcrumbs.Item>
                    <Anchor href="./logout">Logout</Anchor>
                </Breadcrumbs.Item>}
            </Breadcrumbs>            
        );
    }

    displayCompaniesButton(authorities) {
        const hasCompanyPermission = ((authorities !== undefined) && authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_COMPANY_READ' 
        || item === 'ROLE_COMPANY_CREATE' || item === 'ROLE_COMPANY_SAVE' || item === 'ROLE_COMPANY_DELETE' || item === 'SCOPE_openid'));
        return (<NavItem eventKey="companies" disabled={(!hasCompanyPermission)}>
            <NavIcon>
                <i className="fa fa-fw fa-building" style={{ fontSize: '1.75em', verticalAlign: 'middle' }} />
            </NavIcon>
            <NavText style={{ paddingRight: 32 }} title="Companies">
                Companies
            </NavText>
        </NavItem>);
    }

    render() {
        const { expanded, selected, authorities, user } = this.state;

        return (
            <div>
                <div>
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
                        {this.displayCompaniesButton(authorities)}
                        {this.displayPostsButton(authorities)}
                        {this.displayProductsButton(authorities)}
                        {this.displayWeekMenuButtons(authorities)}
                        <Separator />
                        {this.displayLogoutButton()}
                        {this.displayPasskeyButton()}
                    </Nav>
                </SideNav>
                </div>
                <Main expanded={expanded}>
                    {this.renderBreadcrumbs()}
                </Main>
            </div>
        );
    }
}

export default withRouter(HomeContent);
