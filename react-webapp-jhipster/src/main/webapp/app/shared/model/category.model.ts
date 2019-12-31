import { Moment } from 'moment';
import { IRecipe } from 'app/shared/model/recipe.model';
import { IProduct } from 'app/shared/model/product.model';

export interface ICategory {
  id?: string;
  name?: string;
  updateDate?: Moment;
  insertDate?: Moment;
  recipe?: IRecipe;
  products?: IProduct[];
}

export const defaultValue: Readonly<ICategory> = {};
