import React, {Component} from 'react';
import { Button, Form, FormGroup, Label } from 'reactstrap';
import Autosuggest from 'react-autosuggest';
import './Search.css';

// https://developer.mozilla.org/en/docs/Web/JavaScript/Guide/Regular_Expressions#Using_Special_Characters
function escapeRegexCharacters(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

class SearchButtonComponent extends Component {
    constructor(props) {
    super(props);
    this.state = {
      suggestions: props.suggestions,
      search: props.search,
      value: '',
      copySuggestions: []
    };
  }

  copySuggestions() {
    const { suggestions, copySuggestions } = this.state;
    if (copySuggestions.length === 0) {
      this.setState({
        copySuggestions: suggestions
      });
    }
  }

  getSuggestions(value) {
    this.copySuggestions();
    const { copySuggestions } = this.state;
    const escapedValue = escapeRegexCharacters(value.trim());
    
    if (escapedValue === '') {
      return [];
    }
  
    const regex = new RegExp('^' + escapedValue, 'i');
  
    return copySuggestions
    .map(section => {
      return {
        title: section.title,
        languages: section.languages.filter(language => regex.test(language.name))
      };
    })
    .filter(section => section.languages.length > 0);
    //return copySuggestions.filter(language => regex.test(language.name));
  }

  handleSearchChange = (event) => {
    const { handleChange } = this.props;
    handleChange(event);
  }

  handleSearchSubmit = (event) => {
    const { handleSubmit } = this.props;
    handleSubmit(event);
  }

  onChange = (event, { newValue, method }) => {
    this.setState({
      value: newValue
    });
    this.handleSearchChange(event);
  };
  
  onSuggestionsFetchRequested = ({ value }) => {
    this.setState({
      suggestions: this.getSuggestions(value)
    });
  };

  onSuggestionsClearRequested = () => {
    this.setState({
      suggestions: []
    });
  };

  getSectionSuggestions(section) {
    return section.languages;
  }

  render() {
      const { suggestions, value } = this.state;

      // When suggestion is clicked, Autosuggest needs to populate the input
      // based on the clicked suggestion. Teach Autosuggest how to calculate the
      // input value for every given suggestion.
      const getSuggestionValue = suggestion => suggestion.name;

      // Use your imagination to render suggestions.
      const renderSuggestion = suggestion => (
        <div>
          {suggestion.name}
        </div>
      );

      const renderSectionTitle = section => (
        <div>
          <strong>{section.title}</strong>
        </div>
      );

      const inputProps = {
        placeholder: 'Search by fields',
        value,
        onChange: this.onChange,
        name: 'search'
      };

      return (<div
      style={{
        padding: '15px 20px 0 20px'
      }}
      ><Form onSubmit={this.handleSearchSubmit} inline>
        <FormGroup className="mb-2 mr-sm-2 mb-sm-0">
          <Label for="search" className="mr-sm-2">Search</Label>
          <Autosuggest
            multiSection={true}
            suggestions={suggestions}
            onSuggestionsFetchRequested={this.onSuggestionsFetchRequested}
            onSuggestionsClearRequested={this.onSuggestionsClearRequested}
            getSuggestionValue={getSuggestionValue}
            renderSuggestion={renderSuggestion}
            renderSectionTitle={renderSectionTitle}
            getSectionSuggestions={this.getSectionSuggestions}
            inputProps={inputProps}
          />          
        </FormGroup>
        <Button>Search</Button>
    </Form></div>)
  }
}

export default SearchButtonComponent;