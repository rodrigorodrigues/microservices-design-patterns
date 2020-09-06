import { Moment } from 'moment';
import { ICategory } from 'app/shared/model/category.model';

export interface IProduct {
  id?: string;
  name?: string;
  insertDate?: Moment;
  completed?: boolean;
  quantity?: number;
  category?: ICategory;
}

export const defaultValue: Readonly<IProduct> = {
  completed: false
};
