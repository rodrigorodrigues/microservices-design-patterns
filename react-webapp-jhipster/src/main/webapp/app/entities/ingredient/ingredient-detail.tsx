import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate, ICrudGetAction, TextFormat } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { IRootState } from 'app/shared/reducers';
import { getEntity } from './ingredient.reducer';
import { IIngredient } from 'app/shared/model/ingredient.model';
import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';

export interface IIngredientDetailProps extends StateProps, DispatchProps, RouteComponentProps<{ id: string }> {}

export class IngredientDetail extends React.Component<IIngredientDetailProps> {
  componentDidMount() {
    this.props.getEntity(this.props.match.params.id);
  }

  render() {
    const { ingredientEntity } = this.props;
    return (
      <Row>
        <Col md="8">
          <h2>
            <Translate contentKey="spendingbetterApp.ingredient.detail.title">Ingredient</Translate> [<b>{ingredientEntity.id}</b>]
          </h2>
          <dl className="jh-entity-details">
            <dt>
              <span id="name">
                <Translate contentKey="spendingbetterApp.ingredient.name">Name</Translate>
              </span>
            </dt>
            <dd>{ingredientEntity.name}</dd>
            <dt>
              <span id="categoryName">
                <Translate contentKey="spendingbetterApp.ingredient.categoryName">Category Name</Translate>
              </span>
            </dt>
            <dd>{ingredientEntity.categoryName}</dd>
            <dt>
              <span id="updateDate">
                <Translate contentKey="spendingbetterApp.ingredient.updateDate">Update Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={ingredientEntity.updateDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
            <dt>
              <span id="insertDate">
                <Translate contentKey="spendingbetterApp.ingredient.insertDate">Insert Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={ingredientEntity.insertDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
            <dt>
              <span id="tempRecipeLinkIndicator">
                <Translate contentKey="spendingbetterApp.ingredient.tempRecipeLinkIndicator">Temp Recipe Link Indicator</Translate>
              </span>
            </dt>
            <dd>{ingredientEntity.tempRecipeLinkIndicator ? 'true' : 'false'}</dd>
            <dt>
              <span id="checkedInCartShopping">
                <Translate contentKey="spendingbetterApp.ingredient.checkedInCartShopping">Checked In Cart Shopping</Translate>
              </span>
            </dt>
            <dd>{ingredientEntity.checkedInCartShopping ? 'true' : 'false'}</dd>
            <dt>
              <span id="updateCheckDate">
                <Translate contentKey="spendingbetterApp.ingredient.updateCheckDate">Update Check Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={ingredientEntity.updateCheckDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
            <dt>
              <span id="expiryDate">
                <Translate contentKey="spendingbetterApp.ingredient.expiryDate">Expiry Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={ingredientEntity.expiryDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
          </dl>
          <Button tag={Link} to="/entity/ingredient" replace color="info">
            <FontAwesomeIcon icon="arrow-left" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.back">Back</Translate>
            </span>
          </Button>
          &nbsp;
          <Button tag={Link} to={`/entity/ingredient/${ingredientEntity.id}/edit`} replace color="primary">
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

const mapStateToProps = ({ ingredient }: IRootState) => ({
  ingredientEntity: ingredient.entity
});

const mapDispatchToProps = { getEntity };

type StateProps = ReturnType<typeof mapStateToProps>;
type DispatchProps = typeof mapDispatchToProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(IngredientDetail);
