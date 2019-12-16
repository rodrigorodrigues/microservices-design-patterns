import axios from 'axios';
import { ICrudGetAction, ICrudGetAllAction, ICrudPutAction, ICrudDeleteAction } from 'react-jhipster';

import { REQUEST, SUCCESS, FAILURE } from 'app/shared/reducers/action-type.util';
import { IUser, defaultValue } from 'app/shared/model/user.model';

import { Storage } from 'react-jhipster';

import sleep from 'await-sleep';

import { ICrudGetAllEventSourceAction } from 'app/shared/reducers/event-source-type.util';

export const ACTION_TYPES = {
  FETCH_ROLES: 'userManagement/FETCH_ROLES',
  FETCH_USERS: 'userManagement/FETCH_USERS',
  FETCH_USER: 'userManagement/FETCH_USER',
  CREATE_USER: 'userManagement/CREATE_USER',
  UPDATE_USER: 'userManagement/UPDATE_USER',
  DELETE_USER: 'userManagement/DELETE_USER',
  RESET: 'userManagement/RESET'
};

const initialState = {
  loading: false,
  errorMessage: null,
  users: [] as ReadonlyArray<IUser>,
  authorities: [] as any[],
  user: defaultValue,
  updating: false,
  updateSuccess: false,
  totalItems: 0
};

export type UserManagementState = Readonly<typeof initialState>;

// Reducer
export default (state: UserManagementState = initialState, action): UserManagementState => {
  switch (action.type) {
    case REQUEST(ACTION_TYPES.FETCH_ROLES):
      return {
        ...state
      };
    case REQUEST(ACTION_TYPES.FETCH_USERS):
    case REQUEST(ACTION_TYPES.FETCH_USER):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        loading: true
      };
    case REQUEST(ACTION_TYPES.CREATE_USER):
    case REQUEST(ACTION_TYPES.UPDATE_USER):
    case REQUEST(ACTION_TYPES.DELETE_USER):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        updating: true
      };
    case FAILURE(ACTION_TYPES.FETCH_USERS):
    case FAILURE(ACTION_TYPES.FETCH_USER):
    case FAILURE(ACTION_TYPES.FETCH_ROLES):
    case FAILURE(ACTION_TYPES.CREATE_USER):
    case FAILURE(ACTION_TYPES.UPDATE_USER):
    case FAILURE(ACTION_TYPES.DELETE_USER):
      return {
        ...state,
        loading: false,
        updating: false,
        updateSuccess: false,
        errorMessage: action.payload
      };
    case SUCCESS(ACTION_TYPES.FETCH_ROLES):
      return {
        ...state,
        authorities: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.FETCH_USERS):
      return {
        ...state,
        loading: false,
        users: action.payload.data,
        totalItems: parseInt(action.payload.headers['x-total-count'], 10)
      };
    case ACTION_TYPES.FETCH_USERS:
      return {
        ...state,
        loading: false,
        users: action.payload,
        totalItems: parseInt(action.payload.length, 10)
      };
    case SUCCESS(ACTION_TYPES.FETCH_USER):
      return {
        ...state,
        loading: false,
        user: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.CREATE_USER):
    case SUCCESS(ACTION_TYPES.UPDATE_USER):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        user: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.DELETE_USER):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        user: defaultValue
      };
    case ACTION_TYPES.RESET:
      return {
        ...initialState
      };
    default:
      return state;
  }
};

const apiUrl = 'api/users';
// Actions

export const getUsersByEventSource: ICrudGetAllEventSourceAction<ReadonlyArray<IUser>> = (page, size, sort) => async dispatch => {
  console.log(`Loading Data...`);

  const AUTH_TOKEN_KEY = 'jhi-authenticationToken';

  let jwt = Storage.local.get(AUTH_TOKEN_KEY);

  if (!jwt) {
    jwt = Storage.session.get(AUTH_TOKEN_KEY);
  }

  jwt = `Bearer ${jwt}`;

  const requestUrl = `${apiUrl}?Authorization=${jwt}${sort ? `&page=${page}&size=${size}&sort=${sort}` : ''}`;

  const eventSource = new EventSource(`${requestUrl}`);

  eventSource.addEventListener('open', result => {
    console.log('EventSource open: ', result);
  });

  const entities = [] as Array<IUser>;

  eventSource.addEventListener('message', (result: any) => {
    console.log(`Event Source Type: ${result}`);
    const data = JSON.parse(result.data);
    console.log(`Event Source Data: ${JSON.stringify(data)}`);
    entities.push(data);
  });

  let isClosed = null;

  eventSource.addEventListener('error', err => {
    console.log('EventSource error: ', err);
    eventSource.close();
    isClosed = new Promise(function(resolve, reject) {
      resolve(true);
    });
  });

  while (isClosed === null) {
    console.log(`Waiting for 1 sec: ${new Date().getTime()}`);

    await sleep(1000);
  }

  const result = await dispatch({
    type: ACTION_TYPES.FETCH_USERS,
    payload: entities
  });

  return result;
};

export const getUsers: ICrudGetAllAction<IUser> = (page, size, sort) => {
  const requestUrl = `${apiUrl}${sort ? `?page=${page}&size=${size}&sort=${sort}` : ''}`;
  return {
    type: ACTION_TYPES.FETCH_USERS,
    payload: axios.get<IUser>(requestUrl)
  };
};

export const getRoles = () => ({
  type: ACTION_TYPES.FETCH_ROLES,
  payload: axios.get(`${apiUrl}/authorities`)
});

export const getUser: ICrudGetAction<IUser> = id => {
  const requestUrl = `${apiUrl}/${id}`;
  return {
    type: ACTION_TYPES.FETCH_USER,
    payload: axios.get<IUser>(requestUrl)
  };
};

export const createUser: ICrudPutAction<IUser> = user => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.CREATE_USER,
    payload: axios.post(apiUrl, user)
  });
  dispatch(getUsers());
  return result;
};

export const updateUser: ICrudPutAction<IUser> = user => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.UPDATE_USER,
    payload: axios.put(apiUrl, user)
  });
  dispatch(getUsers());
  return result;
};

export const deleteUser: ICrudDeleteAction<IUser> = id => async dispatch => {
  const requestUrl = `${apiUrl}/${id}`;
  const result = await dispatch({
    type: ACTION_TYPES.DELETE_USER,
    payload: axios.delete(requestUrl)
  });
  dispatch(getUsers());
  return result;
};

export const reset = () => ({
  type: ACTION_TYPES.RESET
});
