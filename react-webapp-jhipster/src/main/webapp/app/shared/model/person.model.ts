import { Moment } from 'moment';
import { IAddress } from 'app/shared/model/address.model';
import { IChild } from 'app/shared/model/child.model';

export interface IPerson {
  id?: string;
  fullName?: string;
  dateOfBirth?: Moment;
  createdByUser?: string;
  createdDate?: Moment;
  lastModifiedByUser?: string;
  lastModifiedDate?: Moment;
  address?: IAddress;
  children?: IChild[];
}

export const defaultValue: Readonly<IPerson> = {};
