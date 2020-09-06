import React from 'react';
import { Switch } from 'react-router-dom';
import Loadable from 'react-loadable';

import Login from 'app/modules/login/login';
import Register from 'app/modules/account/register/register';
import Activate from 'app/modules/account/activate/activate';
import PasswordResetInit from 'app/modules/account/password-reset/init/password-reset-init';
import PasswordResetFinish from 'app/modules/account/password-reset/finish/password-reset-finish';
import Logout from 'app/modules/login/logout';
import Home from 'app/modules/home/home';
import Entities from 'app/entities';
import PrivateRoute from 'app/shared/auth/private-route';
import ErrorBoundaryRoute from 'app/shared/error/error-boundary-route';
import PageNotFound from 'app/shared/error/page-not-found';
import { AUTHORITIES } from 'app/config/constants';

// tslint:disable:space-in-parens
const Account = Loadable({
  loader: () => import(/* webpackChunkName: "account" */ 'app/modules/account'),
  loading: () => <div>loading ...</div>
});

const Admin = Loadable({
  loader: () => import(/* webpackChunkName: "administration" */ 'app/modules/administration'),
  loading: () => <div>loading ...</div>
});

const Person = Loadable({
  loader: () => import(/* webpackChunkName: "person" */ 'app/entities/person'),
  loading: () => <div>loading ...</div>
});

const Category = Loadable({
  loader: () => import(/* webpackChunkName: "category" */ 'app/entities/category'),
  loading: () => <div>loading ...</div>
});

const Task = Loadable({
  loader: () => import(/* webpackChunkName: "task" */ 'app/entities/task'),
  loading: () => <div>loading ...</div>
});

const Ingredient = Loadable({
  loader: () => import(/* webpackChunkName: "ingredient" */ 'app/entities/ingredient'),
  loading: () => <div>loading ...</div>
});

const Product = Loadable({
  loader: () => import(/* webpackChunkName: "product" */ 'app/entities/product'),
  loading: () => <div>loading ...</div>
});

const Recipe = Loadable({
  loader: () => import(/* webpackChunkName: "recipe" */ 'app/entities/recipe'),
  loading: () => <div>loading ...</div>
});
// tslint:enable

const Routes = () => (
  <div className="view-routes">
    <Switch>
      <ErrorBoundaryRoute path="/login" component={Login} />
      <ErrorBoundaryRoute path="/logout" component={Logout} />
      <ErrorBoundaryRoute path="/register" component={Register} />
      <ErrorBoundaryRoute path="/activate/:key?" component={Activate} />
      <ErrorBoundaryRoute path="/reset/request" component={PasswordResetInit} />
      <ErrorBoundaryRoute path="/reset/finish/:key?" component={PasswordResetFinish} />
      <PrivateRoute path="/admin" component={Admin} hasAnyAuthorities={[AUTHORITIES.ADMIN]} />
      <PrivateRoute path="/account" component={Account} hasAnyAuthorities={[AUTHORITIES.ADMIN, AUTHORITIES.USER]} />
      <PrivateRoute
        path="/entity/person"
        component={Person}
        hasAnyAuthorities={[
          AUTHORITIES.ADMIN,
          AUTHORITIES.PERSON_CREATE,
          AUTHORITIES.PERSON_DELETE,
          AUTHORITIES.PERSON_READ,
          AUTHORITIES.PERSON_SAVE
        ]}
      />
      <PrivateRoute
        path="/entity/category"
        component={Category}
        hasAnyAuthorities={[
          AUTHORITIES.ADMIN,
          AUTHORITIES.CATEGORY_CREATE,
          AUTHORITIES.CATEGORY_DELETE,
          AUTHORITIES.CATEGORY_READ,
          AUTHORITIES.CATEGORY_SAVE
        ]}
      />
      <PrivateRoute
        path="/entity/task"
        component={Task}
        hasAnyAuthorities={[
          AUTHORITIES.ADMIN,
          AUTHORITIES.TASK_CREATE,
          AUTHORITIES.TASK_DELETE,
          AUTHORITIES.TASK_READ,
          AUTHORITIES.TASK_SAVE
        ]}
      />
      <PrivateRoute
        path="/entity/ingredient"
        component={Ingredient}
        hasAnyAuthorities={[
          AUTHORITIES.ADMIN,
          AUTHORITIES.INGREDIENT_CREATE,
          AUTHORITIES.INGREDIENT_DELETE,
          AUTHORITIES.INGREDIENT_READ,
          AUTHORITIES.INGREDIENT_SAVE
        ]}
      />
      <PrivateRoute
        path="/entity/product"
        component={Product}
        hasAnyAuthorities={[
          AUTHORITIES.ADMIN,
          AUTHORITIES.PRODUCT_CREATE,
          AUTHORITIES.PRODUCT_DELETE,
          AUTHORITIES.PRODUCT_READ,
          AUTHORITIES.PRODUCT_SAVE
        ]}
      />
      <PrivateRoute
        path="/entity/recipe"
        component={Recipe}
        hasAnyAuthorities={[
          AUTHORITIES.ADMIN,
          AUTHORITIES.RECIPE_CREATE,
          AUTHORITIES.RECIPE_DELETE,
          AUTHORITIES.RECIPE_READ,
          AUTHORITIES.RECIPE_SAVE
        ]}
      />
      <ErrorBoundaryRoute path="/" exact component={Home} />
      <ErrorBoundaryRoute component={PageNotFound} />
    </Switch>
  </div>
);

export default Routes;
