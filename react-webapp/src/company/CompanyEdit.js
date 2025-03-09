import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {Feedback, Form, FormGroup, Input, Label} from '@availity/form';
import {Button, Container, UncontrolledAlert} from 'reactstrap';
import * as yup from 'yup';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import FooterContent from '../home/FooterContent';
import HomeContent from '../home/HomeContent';
import { marginLeft } from '../common/Util';
import { loading } from '../common/Loading';
import uuid from 'react-uuid';

class CompanyEdit extends Component {
  emptyCompany = {
    name: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      company: this.emptyCompany,
      jwt: props.jwt,
      displayError: null,
      displayAlert: false,
      authorities: props.authorities,
      isLoading: false,
      expanded: false,
      isAuthenticated: props.isAuthenticated,
      gatewayUrl: props.gatewayUrl
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  setExpanded = (expanded) => {
    this.setState({expanded: expanded});
  }

  async componentDidMount() {
    try {
      this.setLoading(true);
      const { jwt, authorities, gatewayUrl } = this.state;
      if (jwt && authorities) {
        if (!authorities.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_COMPANY_CREATE' || item === 'ROLE_COMPANY_SAVE' || item === 'SCOPE_openid')) {
          const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
          this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
        } else {
          if (this.props.match.params.id !== 'new') {
            try {
              const company = await (await fetch(`${gatewayUrl}/api/companies/${this.props.match.params.id}`, { method: 'GET',      headers: {
                'Content-Type': 'application/json',
                'Authorization': jwt
              }})).json();
              this.setState({company: company, isLoading: false});
            } catch (error) {
              this.setState({displayAlert: true, sLoading: false, displayError: errorMessage(error)});
            }
          } else {
            this.setState({isLoading: false});
          }
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let company = {...this.state.company};
    company[name] = value;
    this.setState({company: company});
  }

  async handleSubmit(event) {
    try {
      const {company, jwt} = this.state;

      const url = '/api/companies' + (company.id ? '/' + company.id : '');
      await fetch(url, {
        method: (company.id) ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': jwt,
          'requestId': uuid()
        },
        body: JSON.stringify(company),
        credentials: 'include'
      }).then(response => response.json())
          .then(data => {
            if (data.id) {
              this.props.history.push('/companies');
            } else {
              this.setState({ displayError: errorMessage(data)});
            }
          })
          .catch((error) => {
            this.setState({ displayError: errorMessage(error)});
          });
    } finally {
      this.setLoading(false);
    }
  }

  setLoading = (loading) => {
    this.setState({ isLoading: loading });
    console.log("setLoading: " + loading);
  }

  render() {
    const { company, displayError, displayAlert, isLoading, expanded } = this.state;
    const title = <h2>{company.id ? 'Edit Company' : 'Add Company'}</h2>;

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
          {title}
          <Form onSubmit={this.handleSubmit}
            enableReinitialize={true}
            initialValues={{
              name: company?.name || ''
            }}
            validationSchema={yup.object().shape({
              name: yup.string().trim().required('This field is required.')
            })}
          >
            <FormGroup>
              <Label for="name">Name</Label>
              <Input type="text" name="name" id="name" value={company.name || ''}
                    onChange={this.handleChange} placeholder="Name"
                      required/>
              <Feedback name="name">
                This field is invalid
              </Feedback>
            </FormGroup>
            <FormGroup>
              <Button color="primary" type="submit">{company.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/companies">Cancel</Button>
            </FormGroup>
          </Form>
          </div>
      }
    }

    return <div
    style={{
      marginLeft: marginLeft(expanded),
      padding: '15px 20px 0 20px'
    }}
  >
      <AppNavbar/>
      <Container fluid>
        <HomeContent setExpanded={this.setExpanded} {...this.state}></HomeContent>
        {loading(isLoading)}
        {!isLoading && displayContent()}
        <MessageAlert {...displayError}></MessageAlert>
        <FooterContent></FooterContent>
      </Container>
    </div>
  }
}

export default withRouter(CompanyEdit);