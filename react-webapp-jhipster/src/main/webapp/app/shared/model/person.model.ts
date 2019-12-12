import { Moment } from 'moment';

export interface IPerson {
  id?: string;
  fullName?: string;
  dateOfBirth?: Moment;
  createdByUser?: string;
  createdDate?: Moment;
  lastModifiedByUser?: string;
  lastModifiedDate?: Moment;
}

export const defaultValue: Readonly<IPerson> = {};
