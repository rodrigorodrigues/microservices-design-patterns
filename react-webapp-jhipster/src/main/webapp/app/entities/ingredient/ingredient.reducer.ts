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

import { IIngredient, defaultValue } from 'app/shared/model/ingredient.model';

export const ACTION_TYPES = {
  FETCH_INGREDIENT_LIST: 'ingredient/FETCH_INGREDIENT_LIST',
  FETCH_INGREDIENT: 'ingredient/FETCH_INGREDIENT',
  CREATE_INGREDIENT: 'ingredient/CREATE_INGREDIENT',
  UPDATE_INGREDIENT: 'ingredient/UPDATE_INGREDIENT',
  DELETE_INGREDIENT: 'ingredient/DELETE_INGREDIENT',
  RESET: 'ingredient/RESET'
};

const initialState = {
  loading: false,
  errorMessage: null,
  entities: [] as ReadonlyArray<IIngredient>,
  entity: defaultValue,
  links: { next: 0 },
  updating: false,
  totalItems: 0,
  updateSuccess: false
};

export type IngredientState = Readonly<typeof initialState>;

// Reducer

export default (state: IngredientState = initialState, action): IngredientState => {
  switch (action.type) {
    case REQUEST(ACTION_TYPES.FETCH_INGREDIENT_LIST):
    case REQUEST(ACTION_TYPES.FETCH_INGREDIENT):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        loading: true
      };
    case REQUEST(ACTION_TYPES.CREATE_INGREDIENT):
    case REQUEST(ACTION_TYPES.UPDATE_INGREDIENT):
    case REQUEST(ACTION_TYPES.DELETE_INGREDIENT):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        updating: true
      };
    case FAILURE(ACTION_TYPES.FETCH_INGREDIENT_LIST):
    case FAILURE(ACTION_TYPES.FETCH_INGREDIENT):
    case FAILURE(ACTION_TYPES.CREATE_INGREDIENT):
    case FAILURE(ACTION_TYPES.UPDATE_INGREDIENT):
    case FAILURE(ACTION_TYPES.DELETE_INGREDIENT):
      return {
        ...state,
        loading: false,
        updating: false,
        updateSuccess: false,
        errorMessage: action.payload
      };
    case SUCCESS(ACTION_TYPES.FETCH_INGREDIENT_LIST): {
      const links = parseHeaderForLinks(action.payload.headers.link);

      return {
        ...state,
        loading: false,
        links,
        entities: loadMoreDataWhenScrolled(state.entities, action.payload.data, links),
        totalItems: parseInt(action.payload.headers['x-total-count'], 10)
      };
    }
    case SUCCESS(ACTION_TYPES.FETCH_INGREDIENT):
      return {
        ...state,
        loading: false,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.CREATE_INGREDIENT):
    case SUCCESS(ACTION_TYPES.UPDATE_INGREDIENT):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.DELETE_INGREDIENT):
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

const apiUrl = 'api/ingredients';

// Actions

export const getEntities: ICrudGetAllAction<IIngredient> = (page, size, sort) => {
  const requestUrl = `${apiUrl}${sort ? `?page=${page}&size=${size}&sort=${sort}` : ''}`;
  return {
    type: ACTION_TYPES.FETCH_INGREDIENT_LIST,
    payload: axios.get<IIngredient>(`${requestUrl}${sort ? '&' : '?'}cacheBuster=${new Date().getTime()}`)
  };
};

export const getEntity: ICrudGetAction<IIngredient> = id => {
  const requestUrl = `${apiUrl}/${id}`;
  return {
    type: ACTION_TYPES.FETCH_INGREDIENT,
    payload: axios.get<IIngredient>(requestUrl)
  };
};

export const createEntity: ICrudPutAction<IIngredient> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.CREATE_INGREDIENT,
    payload: axios.post(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const updateEntity: ICrudPutAction<IIngredient> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.UPDATE_INGREDIENT,
    payload: axios.put(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const deleteEntity: ICrudDeleteAction<IIngredient> = id => async dispatch => {
  const requestUrl = `${apiUrl}/${id}`;
  const result = await dispatch({
    type: ACTION_TYPES.DELETE_INGREDIENT,
    payload: axios.delete(requestUrl)
  });
  return result;
};

export const reset = () => ({
  type: ACTION_TYPES.RESET
});
