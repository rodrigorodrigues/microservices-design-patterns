import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import Child from './child';
import ChildDetail from './child-detail';
import ChildUpdate from './child-update';
import ChildDeleteDialog from './child-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={ChildUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={ChildUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={ChildDetail} />
      <ErrorBoundaryRoute path={match.url} component={Child} />
    </Switch>
    <ErrorBoundaryRoute path={`${match.url}/:id/delete`} component={ChildDeleteDialog} />
  </>
);

export default Routes;
