import { IPerson } from 'app/shared/model/person.model';

export interface IAddress {
  id?: string;
  address?: string;
  postalCode?: string;
  city?: string;
  stateOrProvince?: string;
  country?: string;
  person?: IPerson;
}

export const defaultValue: Readonly<IAddress> = {};
