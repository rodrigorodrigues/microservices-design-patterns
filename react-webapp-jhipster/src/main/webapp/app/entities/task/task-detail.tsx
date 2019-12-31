import React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import { Translate, ICrudGetAction, TextFormat } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { IRootState } from 'app/shared/reducers';
import { getEntity } from './task.reducer';
import { ITask } from 'app/shared/model/task.model';
import { APP_DATE_FORMAT, APP_LOCAL_DATE_FORMAT } from 'app/config/constants';

export interface ITaskDetailProps extends StateProps, DispatchProps, RouteComponentProps<{ id: string }> {}

export class TaskDetail extends React.Component<ITaskDetailProps> {
  componentDidMount() {
    this.props.getEntity(this.props.match.params.id);
  }

  render() {
    const { taskEntity } = this.props;
    return (
      <Row>
        <Col md="8">
          <h2>
            <Translate contentKey="spendingbetterApp.task.detail.title">Task</Translate> [<b>{taskEntity.id}</b>]
          </h2>
          <dl className="jh-entity-details">
            <dt>
              <span id="name">
                <Translate contentKey="spendingbetterApp.task.name">Name</Translate>
              </span>
            </dt>
            <dd>{taskEntity.name}</dd>
            <dt>
              <span id="createdByUser">
                <Translate contentKey="spendingbetterApp.task.createdByUser">Created By User</Translate>
              </span>
            </dt>
            <dd>{taskEntity.createdByUser}</dd>
            <dt>
              <span id="createdDate">
                <Translate contentKey="spendingbetterApp.task.createdDate">Created Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={taskEntity.createdDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
            <dt>
              <span id="lastModifiedByUser">
                <Translate contentKey="spendingbetterApp.task.lastModifiedByUser">Last Modified By User</Translate>
              </span>
            </dt>
            <dd>{taskEntity.lastModifiedByUser}</dd>
            <dt>
              <span id="lastModifiedDate">
                <Translate contentKey="spendingbetterApp.task.lastModifiedDate">Last Modified Date</Translate>
              </span>
            </dt>
            <dd>
              <TextFormat value={taskEntity.lastModifiedDate} type="date" format={APP_DATE_FORMAT} />
            </dd>
          </dl>
          <Button tag={Link} to="/entity/task" replace color="info">
            <FontAwesomeIcon icon="arrow-left" />{' '}
            <span className="d-none d-md-inline">
              <Translate contentKey="entity.action.back">Back</Translate>
            </span>
          </Button>
          &nbsp;
          <Button tag={Link} to={`/entity/task/${taskEntity.id}/edit`} replace color="primary">
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

const mapStateToProps = ({ task }: IRootState) => ({
  taskEntity: task.entity
});

const mapDispatchToProps = { getEntity };

type StateProps = ReturnType<typeof mapStateToProps>;
type DispatchProps = typeof mapDispatchToProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(TaskDetail);
