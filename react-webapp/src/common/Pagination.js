import React, {Component} from 'react';
import Pagination from "react-js-pagination";

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