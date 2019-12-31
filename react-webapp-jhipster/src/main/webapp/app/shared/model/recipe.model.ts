import { Moment } from 'moment';
import { ICategory } from 'app/shared/model/category.model';

export interface IRecipe {
  id?: string;
  name?: string;
  updateDate?: Moment;
  insertDate?: Moment;
  categories?: ICategory[];
}

export const defaultValue: Readonly<IRecipe> = {};
