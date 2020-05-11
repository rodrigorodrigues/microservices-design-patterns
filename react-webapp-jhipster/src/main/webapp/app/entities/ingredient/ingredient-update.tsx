import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col, Label } from 'reactstrap';
import { AvFeedback, AvForm, AvGroup, AvInput, AvField } from 'availity-reactstrap-validation';
import { Translate, translate, ICrudGetAction, ICrudGetAllAction, ICrudPutAction } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IRootState } from 'app/shared/reducers';

import { getEntity, updateEntity, createEntity, reset } from './ingredient.reducer';
import { IIngredient } from 'app/shared/model/ingredient.model';
import { convertDateTimeFromServer, convertDateTimeToServer } from 'app/shared/util/date-utils';
import { mapIdList } from 'app/shared/util/entity-utils';

export interface IIngredientUpdateProps extends StateProps, DispatchProps, RouteComponentProps<{ id: string }> {}

export interface IIngredientUpdateState {
  isNew: boolean;
}

export class IngredientUpdate extends React.Component<IIngredientUpdateProps, IIngredientUpdateState> {
  constructor(props) {
    super(props);
    this.state = {
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
  }

  saveEntity = (event, errors, values) => {
    values.updateDate = convertDateTimeToServer(values.updateDate);
    values.insertDate = convertDateTimeToServer(values.insertDate);
    values.updateCheckDate = convertDateTimeToServer(values.updateCheckDate);
    values.expiryDate = convertDateTimeToServer(values.expiryDate);

    if (errors.length === 0) {
      const { ingredientEntity } = this.props;
      const entity = {
        ...ingredientEntity,
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
    this.props.history.push('/entity/ingredient');
  };

  render() {
    const { ingredientEntity, loading, updating } = this.props;
    const { isNew } = this.state;

    return (
      <div>
        <Row className="justify-content-center">
          <Col md="8">
            <h2 id="spendingbetterApp.ingredient.home.createOrEditLabel">
              <Translate contentKey="spendingbetterApp.ingredient.home.createOrEditLabel">Create or edit a Ingredient</Translate>
            </h2>
          </Col>
        </Row>
        <Row className="justify-content-center">
          <Col md="8">
            {loading ? (
              <p>Loading...</p>
            ) : (
              <AvForm model={isNew ? {} : ingredientEntity} onSubmit={this.saveEntity}>
                {!isNew ? (
                  <AvGroup>
                    <Label for="ingredient-id">
                      <Translate contentKey="global.field.id">ID</Translate>
                    </Label>
                    <AvInput id="ingredient-id" type="text" className="form-control" name="id" required readOnly />
                  </AvGroup>
                ) : null}
                <AvGroup>
                  <Label id="nameLabel" for="ingredient-name">
                    <Translate contentKey="spendingbetterApp.ingredient.name">Name</Translate>
                  </Label>
                  <AvField
                    id="ingredient-name"
                    type="text"
                    name="name"
                    validate={{
                      required: { value: true, errorMessage: translate('entity.validation.required') }
                    }}
                  />
                </AvGroup>
                <AvGroup>
                  <Label id="categoryNameLabel" for="ingredient-categoryName">
                    <Translate contentKey="spendingbetterApp.ingredient.categoryName">Category Name</Translate>
                  </Label>
                  <AvField
                    id="ingredient-categoryName"
                    type="text"
                    name="categoryName"
                    validate={{
                      minLength: { value: 1, errorMessage: translate('entity.validation.minlength', { min: 1 }) }
                    }}
                  />
                </AvGroup>
                <AvGroup>
                  <Label id="updateDateLabel" for="ingredient-updateDate">
                    <Translate contentKey="spendingbetterApp.ingredient.updateDate">Update Date</Translate>
                  </Label>
                  <AvInput
                    id="ingredient-updateDate"
                    type="datetime-local"
                    className="form-control"
                    name="updateDate"
                    placeholder={'YYYY-MM-DD HH:mm'}
                    value={isNew ? null : convertDateTimeFromServer(this.props.ingredientEntity.updateDate)}
                  />
                </AvGroup>
                <AvGroup>
                  <Label id="insertDateLabel" for="ingredient-insertDate">
                    <Translate contentKey="spendingbetterApp.ingredient.insertDate">Insert Date</Translate>
                  </Label>
                  <AvInput
                    id="ingredient-insertDate"
                    type="datetime-local"
                    className="form-control"
                    name="insertDate"
                    placeholder={'YYYY-MM-DD HH:mm'}
                    value={isNew ? null : convertDateTimeFromServer(this.props.ingredientEntity.insertDate)}
                  />
                </AvGroup>
                <AvGroup>
                  <Label id="tempRecipeLinkIndicatorLabel" check>
                    <AvInput
                      id="ingredient-tempRecipeLinkIndicator"
                      type="checkbox"
                      className="form-control"
                      name="tempRecipeLinkIndicator"
                    />
                    <Translate contentKey="spendingbetterApp.ingredient.tempRecipeLinkIndicator">Temp Recipe Link Indicator</Translate>
                  </Label>
                </AvGroup>
                <AvGroup>
                  <Label id="checkedInCartShoppingLabel" check>
                    <AvInput id="ingredient-checkedInCartShopping" type="checkbox" className="form-control" name="checkedInCartShopping" />
                    <Translate contentKey="spendingbetterApp.ingredient.checkedInCartShopping">Checked In Cart Shopping</Translate>
                  </Label>
                </AvGroup>
                <AvGroup>
                  <Label id="updateCheckDateLabel" for="ingredient-updateCheckDate">
                    <Translate contentKey="spendingbetterApp.ingredient.updateCheckDate">Update Check Date</Translate>
                  </Label>
                  <AvInput
                    id="ingredient-updateCheckDate"
                    type="datetime-local"
                    className="form-control"
                    name="updateCheckDate"
                    placeholder={'YYYY-MM-DD HH:mm'}
                    value={isNew ? null : convertDateTimeFromServer(this.props.ingredientEntity.updateCheckDate)}
                  />
                </AvGroup>
                <AvGroup>
                  <Label id="expiryDateLabel" for="ingredient-expiryDate">
                    <Translate contentKey="spendingbetterApp.ingredient.expiryDate">Expiry Date</Translate>
                  </Label>
                  <AvInput
                    id="ingredient-expiryDate"
                    type="datetime-local"
                    className="form-control"
                    name="expiryDate"
                    placeholder={'YYYY-MM-DD HH:mm'}
                    value={isNew ? null : convertDateTimeFromServer(this.props.ingredientEntity.expiryDate)}
                  />
                </AvGroup>
                <Button tag={Link} id="cancel-save" to="/entity/ingredient" replace color="info">
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
  ingredientEntity: storeState.ingredient.entity,
  loading: storeState.ingredient.loading,
  updating: storeState.ingredient.updating,
  updateSuccess: storeState.ingredient.updateSuccess
});

const mapDispatchToProps = {
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
)(IngredientUpdate);
