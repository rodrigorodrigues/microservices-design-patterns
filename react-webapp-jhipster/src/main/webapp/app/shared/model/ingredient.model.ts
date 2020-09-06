import { Moment } from 'moment';

export interface IIngredient {
  id?: string;
  name?: string;
  categoryName?: string;
  updateDate?: Moment;
  insertDate?: Moment;
  tempRecipeLinkIndicator?: boolean;
  checkedInCartShopping?: boolean;
  updateCheckDate?: Moment;
  expiryDate?: Moment;
}

export const defaultValue: Readonly<IIngredient> = {
  tempRecipeLinkIndicator: false,
  checkedInCartShopping: false
};
