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

import { IRecipe, defaultValue } from 'app/shared/model/recipe.model';

export const ACTION_TYPES = {
  FETCH_RECIPE_LIST: 'recipe/FETCH_RECIPE_LIST',
  FETCH_RECIPE: 'recipe/FETCH_RECIPE',
  CREATE_RECIPE: 'recipe/CREATE_RECIPE',
  UPDATE_RECIPE: 'recipe/UPDATE_RECIPE',
  DELETE_RECIPE: 'recipe/DELETE_RECIPE',
  RESET: 'recipe/RESET'
};

const initialState = {
  loading: false,
  errorMessage: null,
  entities: [] as ReadonlyArray<IRecipe>,
  entity: defaultValue,
  links: { next: 0 },
  updating: false,
  totalItems: 0,
  updateSuccess: false
};

export type RecipeState = Readonly<typeof initialState>;

// Reducer

export default (state: RecipeState = initialState, action): RecipeState => {
  switch (action.type) {
    case REQUEST(ACTION_TYPES.FETCH_RECIPE_LIST):
    case REQUEST(ACTION_TYPES.FETCH_RECIPE):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        loading: true
      };
    case REQUEST(ACTION_TYPES.CREATE_RECIPE):
    case REQUEST(ACTION_TYPES.UPDATE_RECIPE):
    case REQUEST(ACTION_TYPES.DELETE_RECIPE):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        updating: true
      };
    case FAILURE(ACTION_TYPES.FETCH_RECIPE_LIST):
    case FAILURE(ACTION_TYPES.FETCH_RECIPE):
    case FAILURE(ACTION_TYPES.CREATE_RECIPE):
    case FAILURE(ACTION_TYPES.UPDATE_RECIPE):
    case FAILURE(ACTION_TYPES.DELETE_RECIPE):
      return {
        ...state,
        loading: false,
        updating: false,
        updateSuccess: false,
        errorMessage: action.payload
      };
    case SUCCESS(ACTION_TYPES.FETCH_RECIPE_LIST): {
      const links = parseHeaderForLinks(action.payload.headers.link);

      return {
        ...state,
        loading: false,
        links,
        entities: loadMoreDataWhenScrolled(state.entities, action.payload.data, links),
        totalItems: parseInt(action.payload.headers['x-total-count'], 10)
      };
    }
    case SUCCESS(ACTION_TYPES.FETCH_RECIPE):
      return {
        ...state,
        loading: false,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.CREATE_RECIPE):
    case SUCCESS(ACTION_TYPES.UPDATE_RECIPE):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: action.payload.data
      };
    case SUCCESS(ACTION_TYPES.DELETE_RECIPE):
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

const apiUrl = 'api/recipes';

// Actions

export const getEntities: ICrudGetAllAction<IRecipe> = (page, size, sort) => {
  const requestUrl = `${apiUrl}${sort ? `?page=${page}&size=${size}&sort=${sort}` : ''}`;
  return {
    type: ACTION_TYPES.FETCH_RECIPE_LIST,
    payload: axios.get<IRecipe>(`${requestUrl}${sort ? '&' : '?'}cacheBuster=${new Date().getTime()}`)
  };
};

export const getEntity: ICrudGetAction<IRecipe> = id => {
  const requestUrl = `${apiUrl}/${id}`;
  return {
    type: ACTION_TYPES.FETCH_RECIPE,
    payload: axios.get<IRecipe>(requestUrl)
  };
};

export const createEntity: ICrudPutAction<IRecipe> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.CREATE_RECIPE,
    payload: axios.post(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const updateEntity: ICrudPutAction<IRecipe> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.UPDATE_RECIPE,
    payload: axios.put(apiUrl, cleanEntity(entity))
  });
  return result;
};

export const deleteEntity: ICrudDeleteAction<IRecipe> = id => async dispatch => {
  const requestUrl = `${apiUrl}/${id}`;
  const result = await dispatch({
    type: ACTION_TYPES.DELETE_RECIPE,
    payload: axios.delete(requestUrl)
  });
  return result;
};

export const reset = () => ({
  type: ACTION_TYPES.RESET
});
