import React from 'react';
import MenuItem from 'app/shared/layout/menus/menu-item';
import { DropdownItem } from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Translate, translate } from 'react-jhipster';
import { NavLink as Link } from 'react-router-dom';
import { NavDropdown } from './menu-components';

export const EntitiesMenu = props => (
  // tslint:disable-next-line:jsx-self-close
  <NavDropdown icon="th-list" name={translate('global.menu.entities.main')} id="entity-menu">
    <MenuItem icon="asterisk" to="/entity/person">
      <Translate contentKey="global.menu.entities.person" />
    </MenuItem>
    <MenuItem icon="asterisk" to="/entity/child">
      <Translate contentKey="global.menu.entities.child" />
    </MenuItem>
    <MenuItem icon="asterisk" to="/entity/address">
      <Translate contentKey="global.menu.entities.address" />
    </MenuItem>
    {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
  </NavDropdown>
);
