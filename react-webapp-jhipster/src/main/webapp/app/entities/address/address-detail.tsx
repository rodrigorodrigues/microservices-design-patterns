import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate, ICrudGetAction } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { IRootState } from 'app/shared/reducers';
import { getEntity } from './address.reducer';
import { IAddress } from 'app/shared/model/address.model';
import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';

export interface IAddressDetailProps extends StateProps, DispatchProps, RouteComponentProps<{ id: string }> {}

export class AddressDetail extends React.Component<IAddressDetailProps> {
  componentDidMount() {
    this.props.getEntity(this.props.match.params.id);
  }

  render() {
    const { addressEntity } = this.props;
    return (
      <Row>
        <Col md="8">
          <h2>
            <Translate contentKey="spendingbetterApp.address.detail.title">Address</Translate> [<b>{addressEntity.id}</b>]
          </h2>
          <dl className="jh-entity-details">
            <dt>
              <span id="address">
                <Translate contentKey="spendingbetterApp.address.address">Address</Translate>
              </span>
            </dt>
            <dd>{addressEntity.address}</dd>
            <dt>
              <span id="postalCode">
                <Translate contentKey="spendingbetterApp.address.postalCode">Postal Code</Translate>
              </span>
            </dt>
            <dd>{addressEntity.postalCode}</dd>
            <dt>
              <span id="city">
                <Translate contentKey="spendingbetterApp.address.city">City</Translate>
              </span>
            </dt>
            <dd>{addressEntity.city}</dd>
            <dt>
              <span id="stateOrProvince">
                <Translate contentKey="spendingbetterApp.address.stateOrProvince">State Or Province</Translate>
              </span>
            </dt>
            <dd>{addressEntity.stateOrProvince}</dd>
            <dt>
              <span id="country">
                <Translate contentKey="spendingbetterApp.address.country">Country</Translate>
              </span>
            </dt>
            <dd>{addressEntity.country}</dd>
            <dt>
              <Translate contentKey="spendingbetterApp.address.person">Person</Translate>
            </dt>
            <dd>{addressEntity.person ? addressEntity.person.id : ''}</dd>
          </dl>
          <Button tag={Link} to="/entity/address" replace color="info">
            <FontAwesomeIcon icon="arrow-left" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.back">Back</Translate>
            </span>
          </Button>
          &nbsp;
          <Button tag={Link} to={`/entity/address/${addressEntity.id}/edit`} replace color="primary">
            <FontAwesomeIcon icon="pencil-alt" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.edit">Edit</Translate>
            </span>
          </Button>
        </Col>
      </Row>
    );
  }
}

const mapStateToProps = ({ address }: IRootState) => ({
  addressEntity: address.entity
});

const mapDispatchToProps = { getEntity };

type StateProps = ReturnType<typeof mapStateToProps>;
type DispatchProps = typeof mapDispatchToProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AddressDetail);
