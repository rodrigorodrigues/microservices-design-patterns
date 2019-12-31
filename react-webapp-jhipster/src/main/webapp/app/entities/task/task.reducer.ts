import axios from 'axios';
import {
  parseHeaderForLinks,
  loadMoreDataWhenScrolled,
  ICrudGetAction,
  ICrudGetAllAction,
  ICrudPutAction,
  ICrudDeleteAction
} from 'react-jhipster';

import { cleanEntity } from 'app/shared/util/entity-utils';
import { REQUEST, SUCCESS, FAILURE } from 'app/shared/reducers/action-type.util';

import { ITask, defaultValue } from 'app/shared/model/task.model';

export const ACTION_TYPES = {
  FETCH_TASK_LIST: 'task/FETCH_TASK_LIST',
  FETCH_TASK: 'task/FETCH_TASK',
  CREATE_TASK: 'task/CREATE_TASK',
  UPDATE_TASK: 'task/UPDATE_TASK',
  DELETE_TASK: 'task/DELETE_TASK',
  RESET: 'task/RESET'
};

const initialState = {
  loading: false,
  errorMessage: null,
  entities: [] as ReadonlyArray<ITask>,
  entity: defaultValue,
  links: { next: 0 },
  updating: false,
  totalItems: 0,
  updateSuccess: false
};

export type TaskState = Readonly<typeof initialState>;

// Reducer

export default (state: TaskState = initialState, action): TaskState => {
  switch (action.type) {
    case REQUEST(ACTION_TYPES.FETCH_TASK_LIST):
    case REQUEST(ACTION_TYPES.FETCH_TASK):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        loading: true
      };
    case REQUEST(ACTION_TYPES.CREATE_TASK):
    case REQUEST(ACTION_TYPES.UPDATE_TASK):
    case REQUEST(ACTION_TYPES.DELETE_TASK):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        updating: true
      };
    case FAILURE(ACTION_TYPES.FETCH_TASK_LIST):
    case FAILURE(ACTION_TYPES.FETCH_TASK):
    case FAILURE(ACTION_TYPES.CREATE_TASK):
    case FAILURE(ACTION_TYPES.UPDATE_TASK):
    case FAILURE(ACTION_TYPES.DELETE_TASK):
      return {
        ...state,
        loading: false,
        updating: false,
        updateSuccess: false,
        errorMessage: action.payload
      };
    case SUCCESS(ACTION_TYPES.FETCH_TASK_LIST): {
      const links = parseHeaderForLinks(action.payload.headers.link);

      return {
        ...state,
        loading: false,
        links,
        entities: loadMoreDataWhenScrolled(state.entities, action.payload.data, links),
        totalItems: parseInt(action.payload.headers['x-total-count'], 10)
      };
    }
    case SUCCESS(ACTION_TYPES.FETCH_TASK):
      return {
        ...state,
        loading: false,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.CREATE_TASK):
    case SUCCESS(ACTION_TYPES.UPDATE_TASK):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.DELETE_TASK):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: {}
      };
    case ACTION_TYPES.RESET:
      return {
        ...initialState
      };
    default:
      return state;
  }
};

const apiUrl = 'api/tasks';

// Actions

export const getEntities: ICrudGetAllAction<ITask> = (page, size, sort) => {
  const requestUrl = `${apiUrl}${sort ? `?page=${page}&size=${size}&sort=${sort}` : ''}`;
  return {
    type: ACTION_TYPES.FETCH_TASK_LIST,
    payload: axios.get<ITask>(`${requestUrl}${sort ? '&' : '?'}cacheBuster=${new Date().getTime()}`)
  };
};

export const getEntity: ICrudGetAction<ITask> = id => {
  const requestUrl = `${apiUrl}/${id}`;
  return {
    type: ACTION_TYPES.FETCH_TASK,
    payload: axios.get<ITask>(requestUrl)
  };
};

export const createEntity: ICrudPutAction<ITask> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.CREATE_TASK,
    payload: axios.post(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const updateEntity: ICrudPutAction<ITask> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.UPDATE_TASK,
    payload: axios.put(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const deleteEntity: ICrudDeleteAction<ITask> = id => async dispatch => {
  const requestUrl = `${apiUrl}/${id}`;
  const result = await dispatch({
    type: ACTION_TYPES.DELETE_TASK,
    payload: axios.delete(requestUrl)
  });
  return result;
};

export const reset = () => ({
  type: ACTION_TYPES.RESET
});
