import { Moment } from 'moment';
import { IAddress } from './address.model';

export interface IPerson {
  id?: string;
  fullName?: string;
  dateOfBirth?: Moment;
  createdByUser?: string;
  createdDate?: Moment;
  lastModifiedByUser?: string;
  lastModifiedDate?: Moment;
  address?: IAddress;
}

export const defaultValue: Readonly<IPerson> = {};
