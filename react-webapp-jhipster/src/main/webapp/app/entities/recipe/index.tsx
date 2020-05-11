import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import Recipe from './recipe';
import RecipeDetail from './recipe-detail';
import RecipeUpdate from './recipe-update';
import RecipeDeleteDialog from './recipe-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={RecipeUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={RecipeUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={RecipeDetail} />
      <ErrorBoundaryRoute path={match.url} component={Recipe} />
    </Switch>
    <ErrorBoundaryRoute path={`${match.url}/:id/delete`} component={RecipeDeleteDialog} />
  </>
);

export default Routes;
