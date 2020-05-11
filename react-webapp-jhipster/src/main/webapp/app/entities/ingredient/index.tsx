import React from 'react';
import { Switch } from 'react-router-dom';

import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import Ingredient from './ingredient';
import IngredientDetail from './ingredient-detail';
import IngredientUpdate from './ingredient-update';
import IngredientDeleteDialog from './ingredient-delete-dialog';

const Routes = ({ match }) => (
  <>
    <Switch>
      <ErrorBoundaryRoute exact path={`${match.url}/new`} component={IngredientUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id/edit`} component={IngredientUpdate} />
      <ErrorBoundaryRoute exact path={`${match.url}/:id`} component={IngredientDetail} />
      <ErrorBoundaryRoute path={match.url} component={Ingredient} />
    </Switch>
    <ErrorBoundaryRoute path={`${match.url}/:id/delete`} component={IngredientDeleteDialog} />
  </>
);

export default Routes;
