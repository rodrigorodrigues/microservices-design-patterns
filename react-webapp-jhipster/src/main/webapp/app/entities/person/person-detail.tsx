import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate, ICrudGetAction, TextFormat } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { IRootState } from 'app/shared/reducers';
import { getEntity } from './person.reducer';
import { IPerson } from 'app/shared/model/person.model';
import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';

export interface IPersonDetailProps extends StateProps, DispatchProps, RouteComponentProps<{ id: string }> {}

export class PersonDetail extends React.Component<IPersonDetailProps> {
  componentDidMount() {
    this.props.getEntity(this.props.match.params.id);
  }

  render() {
    const { personEntity } = this.props;
    return (
      <Row>
        <Col md="8">
          <h2>
            <Translate contentKey="spendingbetterApp.person.detail.title">Person</Translate> [<b>{personEntity.id}</b>]
          </h2>
          <dl className="jh-entity-details">
            <dt>
              <span id="fullName">
                <Translate contentKey="spendingbetterApp.person.fullName">Full Name</Translate>
              </span>
            </dt>
            <dd>{personEntity.fullName}</dd>
            <dt>
              <span id="dateOfBirth">
                <Translate contentKey="spendingbetterApp.person.dateOfBirth">Date Of Birth</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={personEntity.dateOfBirth} type="date" format={APP_LOCAL_DATE_FORMAT} />
            </dd>
            <dt>
              <span id="createdByUser">
                <Translate contentKey="spendingbetterApp.person.createdByUser">Created By User</Translate>
              </span>
            </dt>
            <dd>{personEntity.createdByUser}</dd>
            <dt>
              <span id="createdDate">
                <Translate contentKey="spendingbetterApp.person.createdDate">Created Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={personEntity.createdDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
            <dt>
              <span id="lastModifiedByUser">
                <Translate contentKey="spendingbetterApp.person.lastModifiedByUser">Last Modified By User</Translate>
              </span>
            </dt>
            <dd>{personEntity.lastModifiedByUser}</dd>
            <dt>
              <span id="lastModifiedDate">
                <Translate contentKey="spendingbetterApp.person.lastModifiedDate">Last Modified Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={personEntity.lastModifiedDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
          </dl>
          <Button tag={Link} to="/entity/person" replace color="info">
            <FontAwesomeIcon icon="arrow-left" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.back">Back</Translate>
            </span>
          </Button>
          &nbsp;
          <Button tag={Link} to={`/entity/person/${personEntity.id}/edit`} replace color="primary">
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

const mapStateToProps = ({ person }: IRootState) => ({
  personEntity: person.entity
});

const mapDispatchToProps = { getEntity };

type StateProps = ReturnType<typeof mapStateToProps>;
type DispatchProps = typeof mapDispatchToProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(PersonDetail);
