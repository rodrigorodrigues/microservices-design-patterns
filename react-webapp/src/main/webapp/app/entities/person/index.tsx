import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import Person from './person';
import PersonDetail from './person-detail';
import PersonUpdate from './person-update';
import PersonDeleteDialog from './person-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={PersonUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={PersonUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={PersonDetail} />
      <ErrorBoundaryRoute path={match.url} component={Person} />
    </Switch>
    <ErrorBoundaryRoute path={`${match.url}/:id/delete`} component={PersonDeleteDialog} />
  </>
);

export default Routes;
