import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col, Label } from 'reactstrap';
import { AvFeedback, AvForm, AvGroup, AvInput, AvField } from 'availity-reactstrap-validation';
import { Translate, translate, ICrudGetAction, ICrudGetAllAction, ICrudPutAction } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IRootState } from 'app/shared/reducers';

import { IAddress } from 'app/shared/model/address.model';
import { getEntities as getAddresses } from 'app/entities/address/address.reducer';
import { getEntity, updateEntity, createEntity, reset } from './person.reducer';
import { IPerson } from 'app/shared/model/person.model';
import { convertDateTimeFromServer, convertDateTimeToServer } from 'app/shared/util/date-utils';
import { mapIdList } from 'app/shared/util/entity-utils';

export interface IPersonUpdateProps extends StateProps, DispatchProps, RouteComponentProps<{ id: string }> {}

export interface IPersonUpdateState {
  isNew: boolean;
  addressId: string;
}

export class PersonUpdate extends React.Component<IPersonUpdateProps, IPersonUpdateState> {
  constructor(props) {
    super(props);
    this.state = {
      addressId: '0',
      isNew: !this.props.match.params || !this.props.match.params.id
    };
  }

  componentWillUpdate(nextProps, nextState) {
    if (nextProps.updateSuccess !== this.props.updateSuccess && nextProps.updateSuccess) {
      this.handleClose();
    }
  }

  componentDidMount() {
    if (!this.state.isNew) {
      this.props.getEntity(this.props.match.params.id);
    }

    this.props.getAddresses();
  }

  saveEntity = (event, errors, values) => {
    values.createdDate = convertDateTimeToServer(values.createdDate);
    values.lastModifiedDate = convertDateTimeToServer(values.lastModifiedDate);

    if (errors.length === 0) {
      const { personEntity } = this.props;
      const entity = {
        ...personEntity,
        ...values
      };

      if (this.state.isNew) {
        this.props.createEntity(entity);
      } else {
        this.props.updateEntity(entity);
      }
    }
  };

  handleClose = () => {
    this.props.history.push('/entity/person');
  };

  render() {
    const { personEntity, addresses, loading, updating } = this.props;
    const { isNew } = this.state;

    return (
      <div>
        <Row className="justify-content-center">
          <Col md="8">
            <h2 id="spendingbetterApp.person.home.createOrEditLabel">
              <Translate contentKey="spendingbetterApp.person.home.createOrEditLabel">Create or edit a Person</Translate>
            </h2>
          </Col>
        </Row>
        <Row className="justify-content-center">
          <Col md="8">
            {loading ? (
              <p>Loading...</p>
            ) : (
              <AvForm model={isNew ? {} : personEntity} onSubmit={this.saveEntity}>
                {!isNew ? (
                  <AvGroup>
                    <Label for="person-id">
                      <Translate contentKey="global.field.id">ID</Translate>
                    </Label>
                    <AvInput id="person-id" type="text" className="form-control" name="id" required readOnly />
                  </AvGroup>
                ) : null}
                <AvGroup>
                  <Label id="fullNameLabel" for="person-fullName">
                    <Translate contentKey="spendingbetterApp.person.fullName">Full Name</Translate>
                  </Label>
                  <AvField
                    id="person-fullName"
                    type="text"
                    name="fullName"
                    validate={{
                      required: { value: true, errorMessage: translate('entity.validation.required') }
                    }}
                  />
                </AvGroup>
                <AvGroup>
                  <Label id="dateOfBirthLabel" for="person-dateOfBirth">
                    <Translate contentKey="spendingbetterApp.person.dateOfBirth">Date Of Birth</Translate>
                  </Label>
                  <AvField id="person-dateOfBirth" type="date" className="form-control" name="dateOfBirth" />
                </AvGroup>
                <AvGroup>
                  <Label id="createdByUserLabel" for="person-createdByUser">
                    <Translate contentKey="spendingbetterApp.person.createdByUser">Created By User</Translate>
                  </Label>
                  <AvField id="person-createdByUser" type="text" name="createdByUser" />
                </AvGroup>
                <AvGroup>
                  <Label id="createdDateLabel" for="person-createdDate">
                    <Translate contentKey="spendingbetterApp.person.createdDate">Created Date</Translate>
                  </Label>
                  <AvInput
                    id="person-createdDate"
                    type="datetime-local"
                    className="form-control"
                    name="createdDate"
                    placeholder={'YYYY-MM-DD HH:mm'}
                    value={isNew ? null : convertDateTimeFromServer(this.props.personEntity.createdDate)}
                  />
                </AvGroup>
                <AvGroup>
                  <Label id="lastModifiedByUserLabel" for="person-lastModifiedByUser">
                    <Translate contentKey="spendingbetterApp.person.lastModifiedByUser">Last Modified By User</Translate>
                  </Label>
                  <AvField id="person-lastModifiedByUser" type="text" name="lastModifiedByUser" />
                </AvGroup>
                <AvGroup>
                  <Label id="lastModifiedDateLabel" for="person-lastModifiedDate">
                    <Translate contentKey="spendingbetterApp.person.lastModifiedDate">Last Modified Date</Translate>
                  </Label>
                  <AvInput
                    id="person-lastModifiedDate"
                    type="datetime-local"
                    className="form-control"
                    name="lastModifiedDate"
                    placeholder={'YYYY-MM-DD HH:mm'}
                    value={isNew ? null : convertDateTimeFromServer(this.props.personEntity.lastModifiedDate)}
                  />
                </AvGroup>
                <AvGroup>
                  <Label for="person-address">
                    <Translate contentKey="spendingbetterApp.person.address">Address</Translate>
                  </Label>
                  <AvInput id="person-address" type="select" className="form-control" name="address.id">
                    <option value="" key="0" />
                    {addresses
                      ? addresses.map(otherEntity => (
                          <option value={otherEntity.id} key={otherEntity.id}>
                            {otherEntity.id}
                          </option>
                        ))
                      : null}
                  </AvInput>
                </AvGroup>
                <Button tag={Link} id="cancel-save" to="/entity/person" replace color="info">
                  <FontAwesomeIcon icon="arrow-left" />
                  &nbsp;
                  <span className="d-none d-md-inline">
                    <Translate contentKey="entity.action.back">Back</Translate>
                  </span>
                </Button>
                &nbsp;
                <Button color="primary" id="save-entity" type="submit" disabled={updating}>
                  <FontAwesomeIcon icon="save" />
                  &nbsp;
                  <Translate contentKey="entity.action.save">Save</Translate>
                </Button>
              </AvForm>
            )}
          </Col>
        </Row>
      </div>
    );
  }
}

const mapStateToProps = (storeState: IRootState) => ({
  addresses: storeState.address.entities,
  personEntity: storeState.person.entity,
  loading: storeState.person.loading,
  updating: storeState.person.updating,
  updateSuccess: storeState.person.updateSuccess
});

const mapDispatchToProps = {
  getAddresses,
  getEntity,
  updateEntity,
  createEntity,
  reset
};

type StateProps = ReturnType<typeof mapStateToProps>;
type DispatchProps = typeof mapDispatchToProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(PersonUpdate);
