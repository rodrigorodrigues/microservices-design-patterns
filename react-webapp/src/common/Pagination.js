import React, {Component} from 'react';
import Pagination from "react-js-pagination";
import { Input, Col, FormGroup, Label, Row } from 'reactstrap';

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

  handleChangePageSize = (event) => {
    const { setPageSize } = this.props;
    const target = event.target;
    const value = target.value;
    this.setState({pageSize: value});
    setPageSize(value);
  }

  handlePaginationChange = (pageNumber) => {
    console.log(`active page is ${pageNumber}`);
    const { handlePageChange } = this.props;
    handlePageChange(pageNumber);
  }

  render() {
    const { activePage, itemsCountPerPage, totalItemsCount, pageSize } = this.state;

    return (<div className="d-flex justify-content-center">
      <Row form>
        <Col md={2}>
          <FormGroup>
              <Label for="pageSize">Page Size</Label>
              <Input 
                type="text" 
                name="pageSize" 
                id="pageSize" 
                value={pageSize}
                onChange={this.handleChangePageSize} 
              />
          </FormGroup>
        </Col>

        <Col>
          <Label for="pagination">Pagination</Label>
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
        </Col>

        <Col>
          Total: {totalItemsCount}
        </Col>

      </Row>
    </div>);
  }
}

export default PaginationComponent;