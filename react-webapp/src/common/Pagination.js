import React, {Component} from 'react';
import Pagination from "react-js-pagination";
const queryString = require('query-string');

class PaginationComponent extends Component {
    constructor(props) {
    super(props);
    this.state = {
      activePage: props.activePage,
      totalPages: props.totalPages,
      itemsCountPerPage: props.itemsCountPerPage,
      totalItemsCount: props.totalItemsCount,
      pageSize: props.pageSize
    };
  }

  componentDidMount() {
    console.log("Location: "+this.props.location);
    if (this.props.location !== undefined) {
        const parsed = queryString.parse(this.props.location.search);
        if (parsed !== undefined) {  
            if (parsed.page !== undefined) {
            this.setState({activePage: parsed.page});
            }
        }
    }
  }

  handlePaginationChange = (pageNumber) => {
    console.log(`active page is ${pageNumber}`);
    const { handlePageChange } = this.props;
    handlePageChange(pageNumber);
  }

  render() {
    const { activePage, itemsCountPerPage, totalItemsCount } = this.state;

    return (<div className="d-flex justify-content-center">
    <Pagination
        hideNavigation
        activePage={activePage}
        itemsCountPerPage={itemsCountPerPage}
        totalItemsCount={totalItemsCount}
        pageRangeDisplayed={10}
        itemClass='page-item'
        linkClass='btn btn-light'
        onChange={this.handlePaginationChange}
        />
    </div>);
  }
}

export default PaginationComponent;