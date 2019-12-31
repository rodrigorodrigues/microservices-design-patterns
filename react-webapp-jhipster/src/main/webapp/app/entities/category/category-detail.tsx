import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate, ICrudGetAction, TextFormat } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { IRootState } from 'app/shared/reducers';
import { getEntity } from './category.reducer';
import { ICategory } from 'app/shared/model/category.model';
import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';

export interface ICategoryDetailProps extends StateProps, DispatchProps, RouteComponentProps<{ id: string }> {}

export class CategoryDetail extends React.Component<ICategoryDetailProps> {
  componentDidMount() {
    this.props.getEntity(this.props.match.params.id);
  }

  render() {
    const { categoryEntity } = this.props;
    return (
      <Row>
        <Col md="8">
          <h2>
            <Translate contentKey="spendingbetterApp.category.detail.title">Category</Translate> [<b>{categoryEntity.id}</b>]
          </h2>
          <dl className="jh-entity-details">
            <dt>
              <span id="name">
                <Translate contentKey="spendingbetterApp.category.name">Name</Translate>
              </span>
            </dt>
            <dd>{categoryEntity.name}</dd>
            <dt>
              <span id="updateDate">
                <Translate contentKey="spendingbetterApp.category.updateDate">Update Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={categoryEntity.updateDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
            <dt>
              <span id="insertDate">
                <Translate contentKey="spendingbetterApp.category.insertDate">Insert Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={categoryEntity.insertDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
            <dt>
              <Translate contentKey="spendingbetterApp.category.recipe">Recipe</Translate>
            </dt>
            <dd>{categoryEntity.recipe ? categoryEntity.recipe.id : ''}</dd>
          </dl>
          <Button tag={Link} to="/entity/category" replace color="info">
            <FontAwesomeIcon icon="arrow-left" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.back">Back</Translate>
            </span>
          </Button>
          &nbsp;
          <Button tag={Link} to={`/entity/category/${categoryEntity.id}/edit`} replace color="primary">
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

const mapStateToProps = ({ category }: IRootState) => ({
  categoryEntity: category.entity
});

const mapDispatchToProps = { getEntity };

type StateProps = ReturnType<typeof mapStateToProps>;
type DispatchProps = typeof mapDispatchToProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CategoryDetail);
