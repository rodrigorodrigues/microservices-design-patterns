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

import { IChild, defaultValue } from 'app/shared/model/child.model';

export const ACTION_TYPES = {
  FETCH_CHILD_LIST: 'child/FETCH_CHILD_LIST',
  FETCH_CHILD: 'child/FETCH_CHILD',
  CREATE_CHILD: 'child/CREATE_CHILD',
  UPDATE_CHILD: 'child/UPDATE_CHILD',
  DELETE_CHILD: 'child/DELETE_CHILD',
  RESET: 'child/RESET'
};

const initialState = {
  loading: false,
  errorMessage: null,
  entities: [] as ReadonlyArray<IChild>,
  entity: defaultValue,
  links: { next: 0 },
  updating: false,
  totalItems: 0,
  updateSuccess: false
};

export type ChildState = Readonly<typeof initialState>;

// Reducer

export default (state: ChildState = initialState, action): ChildState => {
  switch (action.type) {
    case REQUEST(ACTION_TYPES.FETCH_CHILD_LIST):
    case REQUEST(ACTION_TYPES.FETCH_CHILD):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        loading: true
      };
    case REQUEST(ACTION_TYPES.CREATE_CHILD):
    case REQUEST(ACTION_TYPES.UPDATE_CHILD):
    case REQUEST(ACTION_TYPES.DELETE_CHILD):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        updating: true
      };
    case FAILURE(ACTION_TYPES.FETCH_CHILD_LIST):
    case FAILURE(ACTION_TYPES.FETCH_CHILD):
    case FAILURE(ACTION_TYPES.CREATE_CHILD):
    case FAILURE(ACTION_TYPES.UPDATE_CHILD):
    case FAILURE(ACTION_TYPES.DELETE_CHILD):
      return {
        ...state,
        loading: false,
        updating: false,
        updateSuccess: false,
        errorMessage: action.payload
      };
    case SUCCESS(ACTION_TYPES.FETCH_CHILD_LIST):
      const links = parseHeaderForLinks(action.payload.headers.link);

      return {
        ...state,
        loading: false,
        links,
        entities: loadMoreDataWhenScrolled(state.entities, action.payload.data, links),
        totalItems: parseInt(action.payload.headers['x-total-count'], 10)
      };
    case SUCCESS(ACTION_TYPES.FETCH_CHILD):
      return {
        ...state,
        loading: false,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.CREATE_CHILD):
    case SUCCESS(ACTION_TYPES.UPDATE_CHILD):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.DELETE_CHILD):
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

const apiUrl = 'api/children';

// Actions

export const getEntities: ICrudGetAllAction<IChild> = (page, size, sort) => {
  const requestUrl = `${apiUrl}${sort ? `?page=${page}&size=${size}&sort=${sort}` : ''}`;
  return {
    type: ACTION_TYPES.FETCH_CHILD_LIST,
    payload: axios.get<IChild>(`${requestUrl}${sort ? '&' : '?'}cacheBuster=${new Date().getTime()}`)
  };
};

export const getEntity: ICrudGetAction<IChild> = id => {
  const requestUrl = `${apiUrl}/${id}`;
  return {
    type: ACTION_TYPES.FETCH_CHILD,
    payload: axios.get<IChild>(requestUrl)
  };
};

export const createEntity: ICrudPutAction<IChild> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.CREATE_CHILD,
    payload: axios.post(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const updateEntity: ICrudPutAction<IChild> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.UPDATE_CHILD,
    payload: axios.put(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const deleteEntity: ICrudDeleteAction<IChild> = id => async dispatch => {
  const requestUrl = `${apiUrl}/${id}`;
  const result = await dispatch({
    type: ACTION_TYPES.DELETE_CHILD,
    payload: axios.delete(requestUrl)
  });
  return result;
};

export const reset = () => ({
  type: ACTION_TYPES.RESET
});
