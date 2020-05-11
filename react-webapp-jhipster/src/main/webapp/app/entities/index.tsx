import React from 'react';
import { Switch } from 'react-router-dom';

// tslint:disable-next-line:no-unused-variable
import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';

import Person from './person';
import Ingredient from './ingredient';
import Recipe from './recipe';
import Category from './category';
import Product from './product';
import Task from './task';
/* jhipster-needle-add-route-import - JHipster will add routes here */

const Routes = ({ match }) => (
  <div>
    <Switch>
      {/* prettier-ignore */}
      <ErrorBoundaryRoute path={`${match.url}/person`} component={Person} />
      <ErrorBoundaryRoute path={`${match.url}/ingredient`} component={Ingredient} />
      <ErrorBoundaryRoute path={`${match.url}/recipe`} component={Recipe} />
      <ErrorBoundaryRoute path={`${match.url}/category`} component={Category} />
      <ErrorBoundaryRoute path={`${match.url}/product`} component={Product} />
      <ErrorBoundaryRoute path={`${match.url}/task`} component={Task} />
      {/* jhipster-needle-add-route-path - JHipster will add routes here */}
    </Switch>
  </div>
);

export default Routes;
