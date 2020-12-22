import React, {Component} from 'react';
import { Button, Form, FormGroup, Input, Label } from 'reactstrap';

class SearchButtonComponent extends Component {
    constructor(props) {
    super(props);
    this.state = {
      placeholder: props.placeholder
    };
  }

  handleSearchChange = (event) => {
    const { handleChange } = this.props;
    handleChange(event);
  }

  handleSearchSubmit = (event) => {
    const { handleSubmit } = this.props;
    handleSubmit(event);
  }

  render() {
      const { placeholder } = this.state;
      return (<div
      style={{
        padding: '15px 20px 0 20px'
      }}
      ><Form onSubmit={this.handleSearchSubmit} inline>
        <FormGroup className="mb-2 mr-sm-2 mb-sm-0">
          <Label for="search" className="mr-sm-2">Search</Label>
          <Input 
            type="text" 
            name="search" 
            id="search" 
            onChange={this.handleSearchChange} 
            placeholder={placeholder !== undefined ? placeholder : "name=something"} />
        </FormGroup>
        <Button>Search</Button>
    </Form></div>)
  }
}

export default SearchButtonComponent;