import React, {Component} from 'react';
import {Link, withRouter} from 'react-router-dom';
import {AvFeedback, AvForm, AvGroup, AvInput} from 'availity-reactstrap-validation';
import {Button, Container, Label, UncontrolledAlert} from 'reactstrap';
import AppNavbar from '../home/AppNavbar';
import MessageAlert from '../MessageAlert';
import {errorMessage} from '../common/Util';
import FooterContent from '../home/FooterContent';
import HomeContent from '../home/HomeContent';

class PostEdit extends Component {
  emptyPost = {
    name: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      post: this.emptyPost,
      jwt: props.jwt,
      displayError: null,
      displayAlert: false,
      authorities: props.authorities,
      isLoading: true
    };
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  async componentDidMount() {
    let jwt = this.state.jwt;
    let permissions = this.state.authorities;
    if (jwt && permissions) {

      if (!permissions.some(item => item === 'ROLE_ADMIN' || item === 'ROLE_POST_CREATE' || item === 'ROLE_POST_SAVE')) {
        const jsonError = { 'error': 'You do not have sufficient permission to access this page!' };
        this.setState({displayAlert: true, isLoading: false, displayError: errorMessage(JSON.stringify(jsonError))});
      } else {
        if (this.props.match.params.id !== 'new') {
          try {
            const post = await (await fetch(`/api/posts/${this.props.match.params.id}`, { method: 'GET',      headers: {
              'Content-Type': 'application/json',
              'Authorization': jwt
            }})).json();
            this.setState({post: post, isLoading: false});
          } catch (error) {
            this.setState({displayAlert: true, sLoading: false, displayError: errorMessage(error)});
          }
        } else {
          this.setState({isLoading: false});
        }
      }
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let post = {...this.state.post};
    post[name] = value;
    this.setState({post: post});
  }

  async handleSubmit(event) {
    event.preventDefault();
    const {post, jwt} = this.state;
    console.log("Post", post);
    console.log("Post jwt", jwt);

    const url = '/api/posts' + (post.id ? '/' + post.id : '');
    await fetch(url, {
      method: (post.id) ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': jwt
      },
      body: JSON.stringify(post),
      credentials: 'include'
    }).then(response => response.json())
        .then(data => {
          if (data.id) {
            this.props.history.push('/posts');
          } else {
            this.setState({ displayError: errorMessage(data)});
          }
        })
        .catch((error) => {
          console.log(error);
        });
  }

  render() {
    const {post, displayError, displayAlert, isLoading} = this.state;
    const title = <h2>{post.id ? 'Edit Post' : 'Add Post'}</h2>;

    if (isLoading) {
      return <p>Loading...</p>;
    }

    const displayContent = () => {
      if (displayAlert) {
        return <UncontrolledAlert color="danger">
        401 - Unauthorized - <Button size="sm" color="primary" tag={Link} to={"/logout"}>Please Login Again</Button>
      </UncontrolledAlert>
      } else {
        return <div>
          {title}
          <AvForm onValidSubmit={this.handleSubmit}>
            <AvGroup>
              <Label for="name">Name</Label>
              <AvInput type="text" name="name" id="name" value={post.name || ''}
                    onChange={this.handleChange} placeholder="Name"
                      required/>
              <AvFeedback>
                This field is invalid
              </AvFeedback>
            </AvGroup>
            <AvGroup>
              <Button color="primary" type="submit">{post.id ? 'Save' : 'Create'}</Button>{' '}
              <Button color="secondary" tag={Link} to="/posts">Cancel</Button>
            </AvGroup>
          </AvForm>
          </div>
      }
    }

    return <div>
      <AppNavbar/>
      <Container fluid>
        <HomeContent notDisplayMessage={true}></HomeContent>
        {displayContent()}
        <MessageAlert {...displayError}></MessageAlert>
        <FooterContent></FooterContent>
      </Container>
    </div>
  }
}

export default withRouter(PostEdit);