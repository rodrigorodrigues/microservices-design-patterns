import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table, TabContent, TabPane, Nav, NavItem, NavLink, UncontrolledAlert } from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import { Link, withRouter } from 'react-router-dom';
import MessageAlert from '../MessageAlert';
import { errorMessage } from '../common/Util';
import { getResponse } from "../services/ApiService";
import HomeContent from '../home/HomeContent';
import { confirmDialog } from '../common/ConfirmDialog';
import classnames from 'classnames';
import Iframe from 'react-iframe';
import FooterContent from '../home/FooterContent';
import { toast } from 'react-toastify';
import { marginLeft } from '../common/Util';
import PaginationComponent from "../common/Pagination";
import { loading } from '../common/Loading';
import uuid from 'react-uuid';

const queryString = require('query-string');

const stockSwaggerUrl = process.env.REACT_APP_STOCK_SWAGGER_URL;

export const STOCK_READ_AUTHORITIES = ['ROLE_ADMIN', 'ROLE_STOCKS_READ', 'ROLE_STOCKS_CREATE', 'ROLE_STOCKS_SAVE', 'ROLE_STOCKS_DELETE', 'SCOPE_openid'];

class StockList extends Component {
  constructor(props) {
    super(props);
    this.state = {
      stocks: [],
      isLoading: true,
      jwt: props.jwt,
      displayError: null,
      activeTab: '1',
      authorities: props.authorities,
      displayAlert: false,
      displaySwagger: false,
      expanded: false,
      isAuthenticated: props.isAuthenticated,
      // The pager is 1-based; dotnet-service pages start at 0.
      activePage: 1,
      totalPages: null,
      itemsCountPerPage: null,
      totalItemsCount: null,
      pageSize: 10,
      gatewayUrl: props.gatewayUrl
    };
    this.handlePageChange = this.handlePageChange.bind(this);
    this.remove = this.remove.bind(this);
    this.toggle = this.toggle.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  setPageSize = (pageSize) => {
    this.setState({pageSize: pageSize});
  }

  toggle(tab) {
    if (this.state.activeTab !== tab) {
      this.setState({ activeTab: tab });
    }
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      toast.dismiss('Error');
      let jwt = this.state.jwt;
      let permissions = this.state.authorities;
      if (jwt && permissions) {
        let activePage = this.state.activePage;
        if (window.location.search !== "") {
          const parsed = queryString.parse(window.location.search);
          if (parsed.page !== undefined && Number(parsed.page) > 0) {
            activePage = Number(parsed.page);
            this.setState({activePage: activePage});
          }
        }

        if (!permissions.some(item => STOCK_READ_AUTHORITIES.includes(item))) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          await this.findAllStocks(activePage);
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  async findAllStocks(pageNumber) {
    try {
      const { pageSize, jwt } = this.state;
      const url = `stocks?page=${pageNumber - 1}${pageSize ? '&size=' + pageSize : ''}`;
      const response = await getResponse(url, true, false, jwt, true);
      const data = await response.json();
      if (response.status === 200 && Array.isArray(data.content)) {
        this.setState({
          isLoading: false,
          stocks: data.content,
          displaySwagger: true,
          totalPages: data.totalPages,
          itemsCountPerPage: data.size,
          totalItemsCount: data.totalElements
        });
        this.props.history.push(`/stocks?page=${pageNumber}`);
      } else {
        if (response.status === 401) {
          this.setState({ displayAlert: true });
        }
        this.setState({ isLoading: false, displayError: errorMessage(data) });
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error) });
    }
  }

  async handlePageChange(pageNumber) {
    try {
      this.setLoading(true);
      this.setState({activePage: pageNumber});
      await this.findAllStocks(pageNumber);
    } finally {
      this.setLoading(false);
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  async remove(stock) {
    let confirm = await confirmDialog(`Delete Stock ${stock.name}`, "Are you sure you want to delete this?", "Delete Stock");
    if (confirm) {
      let id = stock.id;
      const { jwt, gatewayUrl } = this.state;
      await fetch(`${gatewayUrl}/api/stocks/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': jwt,
          'requestId': uuid()
        },
        credentials: 'include'
      }).then(async (response) => {
        if (response.status !== 200) {
          this.setState({ displayError: errorMessage(await response.text() || `${response.status} - ${response.statusText}`) });
        } else {
          let stocks = [...this.state.stocks].filter(i => i.id !== id);
          this.setState({ stocks: stocks });
        }
      });
    }
  }

  render() {
    const { stocks, isLoading, displayError, authorities, displayAlert, displaySwagger, expanded } = this.state;

    const hasCreateAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_STOCKS_CREATE' || item === 'SCOPE_openid');

    const hasSaveAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_STOCKS_SAVE' || item === 'SCOPE_openid');

    const hasDeleteAccess = authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_STOCKS_DELETE' || item === 'SCOPE_openid');

    const stockList = stocks.map(stock => {
      return <tr key={stock.id}>
        <th scope="row">{stock.id}</th>
        <td style={{ whiteSpace: 'nowrap' }}>{stock.name}</td>
        <td style={{ whiteSpace: 'nowrap' }}>{stock.category}</td>
        <td>{stock.quantity}</td>
        <td style={{ whiteSpace: 'nowrap' }}>{stock.price} {stock.currency}</td>
        <td>{stock.createdByUser}</td>
        <td>{stock.createdDate}</td>
        <td>{stock.lastModifiedByUser}</td>
        <td>{stock.lastModifiedDate}</td>
        <td>
          <ButtonGroup>
            <Button size="sm" color="primary" tag={Link} to={"/stocks/" + stock.id} disabled={!hasSaveAccess}>Edit</Button>
            <Button size="sm" color="danger" onClick={() => this.remove({ 'id': stock.id, 'name': stock.name })} disabled={!hasDeleteAccess}>Delete</Button>
          </ButtonGroup>
        </td>
      </tr>
    });

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
          <Nav tabs>
        <NavItem>
          <NavLink
            className={classnames({ active: this.state.activeTab === '1' })}
            onClick={() => { this.toggle('1'); }}>
            Stocks
        </NavLink>
        </NavItem>
        {stockSwaggerUrl &&
        <NavItem>
          <NavLink
            className={classnames({ active: this.state.activeTab === '2' })}
            onClick={() => { this.toggle('2'); }}>
            Swagger UI
        </NavLink>
        </NavItem>}
      </Nav>
      <TabContent activeTab={this.state.activeTab}>
        <TabPane tabId="1">
          <div className="float-right"
                style={{
                  padding: '15px 20px 0 20px'
                }}
          >
            <Button color="success" tag={Link} to="/stocks/new" disabled={!hasCreateAccess || displayAlert}>Add Stock</Button>
          </div>
          <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
          <Table striped responsive>
            <thead>
              <tr>
                <th>#</th>
                <th>Name</th>
                <th>Category</th>
                <th>Quantity</th>
                <th>Price</th>
                <th>Created By</th>
                <th>Created Date</th>
                <th>Last Modified By User</th>
                <th>Last Modified By Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {stockList}
            </tbody>
          </Table>
          <PaginationComponent {...this.state} handlePageChange={this.handlePageChange} setPageSize={this.setPageSize} />
        </TabPane>
        <TabPane tabId="2">
          {displaySwagger && stockSwaggerUrl ?
          <Iframe url={`${stockSwaggerUrl}`}
            position="absolute"
            width="100%"
            id="myId"
            className="mt-4"
            height="100%" />
            : null}
        </TabPane>
      </TabContent>
      </div>
      }
    }

    return (
      <div
      style={{
        marginLeft: marginLeft(expanded),
        padding: '15px 20px 0 20px'
      }}
      >
        <AppNavbar />
        <Container fluid>
          <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
          {loading(isLoading)}
          {!isLoading && displayContent()}
          <MessageAlert {...displayError}></MessageAlert>
          <FooterContent></FooterContent>
        </Container>
      </div>
    );
  }
}

export default withRouter(StockList);
