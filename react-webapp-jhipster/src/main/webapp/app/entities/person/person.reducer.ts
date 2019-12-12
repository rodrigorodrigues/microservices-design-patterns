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

import { IPerson, defaultValue } from 'app/shared/model/person.model';

import { Storage } from 'react-jhipster';

import sleep from 'await-sleep';

import { AxiosPromise } from 'axios';

export const ACTION_TYPES = {
  FETCH_PERSON_LIST: 'person/FETCH_PERSON_LIST',
  FETCH_PERSON: 'person/FETCH_PERSON',
  CREATE_PERSON: 'person/CREATE_PERSON',
  UPDATE_PERSON: 'person/UPDATE_PERSON',
  DELETE_PERSON: 'person/DELETE_PERSON',
  RESET: 'person/RESET'
};

const initialState = {
  loading: false,
  errorMessage: null,
  entities: [] as ReadonlyArray<IPerson>,
  entity: defaultValue,
  links: { next: 0 },
  updating: false,
  totalItems: 0,
  updateSuccess: false
};

export type PersonState = Readonly<typeof initialState>;

// Reducer

export default (state: PersonState = initialState, action): PersonState => {
  console.log(`ActionType: ${action.type}`);
  switch (action.type) {
    case REQUEST(ACTION_TYPES.FETCH_PERSON_LIST):
    case REQUEST(ACTION_TYPES.FETCH_PERSON):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        loading: true
      };
    case REQUEST(ACTION_TYPES.CREATE_PERSON):
    case REQUEST(ACTION_TYPES.UPDATE_PERSON):
    case REQUEST(ACTION_TYPES.DELETE_PERSON):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        updating: true
      };
    case FAILURE(ACTION_TYPES.FETCH_PERSON_LIST):
    case FAILURE(ACTION_TYPES.FETCH_PERSON):
    case FAILURE(ACTION_TYPES.CREATE_PERSON):
    case FAILURE(ACTION_TYPES.UPDATE_PERSON):
    case FAILURE(ACTION_TYPES.DELETE_PERSON):
      return {
        ...state,
        loading: false,
        updating: false,
        updateSuccess: false,
        errorMessage: action.payload
      };
    case SUCCESS(ACTION_TYPES.FETCH_PERSON_LIST): {
      const links = parseHeaderForLinks(action.payload.headers.link);

      return {
        ...state,
        loading: false,
        links,
        entities: loadMoreDataWhenScrolled(state.entities, action.payload.data, links),
        totalItems: parseInt(action.payload.headers['x-total-count'], 10)
      };
    }
    case ACTION_TYPES.FETCH_PERSON_LIST: {
      return {
        ...state,
        loading: true,
        entities: loadMoreDataWhenScrolled(action.payload, action.payload, ""),
        totalItems: parseInt(action.payload.length, 10)
      };
    }
    case SUCCESS(ACTION_TYPES.FETCH_PERSON):
      return {
        ...state,
        loading: false,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.CREATE_PERSON):
    case SUCCESS(ACTION_TYPES.UPDATE_PERSON):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.DELETE_PERSON):
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

const apiUrl = 'api/persons';

export interface IPayload<T> {
  type: string;
  payload: AxiosPromise<T>;
  meta?: any;
}
export declare type IPayloadResult<T> = ((dispatch: any) => IPayload<T> | Promise<IPayload<T>>);
export declare type ICrudGetAllEventSourceAction<T> = (page?: number, size?: number, sort?: string) => IPayload<T> | IPayloadResult<T>;

// Actions

export const getEntitiesByEventSource: ICrudGetAllEventSourceAction<ReadonlyArray<IPerson>> = (page, size, sort) => async dispatch => {
  console.log(`Loading Data...`);

  const AUTH_TOKEN_KEY = 'jhi-authenticationToken';

  let jwt = Storage.local.get(AUTH_TOKEN_KEY);

  if (!jwt) {
    jwt = Storage.session.get(AUTH_TOKEN_KEY);
  }

  jwt = `Bearer ${jwt}`;

  const requestUrl = `${apiUrl}?Authorization=${jwt}${sort ? `&page=${page}&size=${size}&sort=${sort}` : ''}`;

  const eventSource = new EventSource(`${requestUrl}`);

  eventSource.addEventListener("open", result => {
    console.log('EventSource open: ', result);
  });

  const entities = [] as Array<IPerson>;

  eventSource.addEventListener("message", (result: any) => {
    console.log(`Event Source Type: ${result}`);
    const data = JSON.parse(result.data);
    console.log(`Event Source Data: ${JSON.stringify(data)}`);
    entities.push(data);
  });

  let isClosed = null;

  eventSource.addEventListener("error", err => {
    console.log('EventSource error: ', err);
    eventSource.close();
    isClosed = new Promise(function (resolve, reject) {
      resolve(true);
    });
  });

  while (isClosed === null) {
    console.log(`Waiting for 1 sec: ${new Date().getTime()}`);
      
    await sleep(1000);
  }

  const result = await dispatch({
    type: ACTION_TYPES.FETCH_PERSON_LIST,
    payload: entities
  });

  return result;
};

export const getEntities: ICrudGetAllAction<IPerson> = (page, size, sort) => {
  const requestUrl = `${apiUrl}${sort ? `?page=${page}&size=${size}&sort=${sort}` : ''}`;
  return {
    type: ACTION_TYPES.FETCH_PERSON_LIST,
    payload: axios.get<IPerson>(`${requestUrl}${sort ? '&' : '?'}cacheBuster=${new Date().getTime()}`)
  };
};

export const getEntity: ICrudGetAction<IPerson> = id => {
  const requestUrl = `${apiUrl}/${id}`;
  return {
    type: ACTION_TYPES.FETCH_PERSON,
    payload: axios.get<IPerson>(requestUrl)
  };
};

export const createEntity: ICrudPutAction<IPerson> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.CREATE_PERSON,
    payload: axios.post(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const updateEntity: ICrudPutAction<IPerson> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.UPDATE_PERSON,
    payload: axios.put(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const deleteEntity: ICrudDeleteAction<IPerson> = id => async dispatch => {
  const requestUrl = `${apiUrl}/${id}`;
  const result = await dispatch({
    type: ACTION_TYPES.DELETE_PERSON,
    payload: axios.delete(requestUrl)
  });
  return result;
};

export const reset = () => ({
  type: ACTION_TYPES.RESET
});
