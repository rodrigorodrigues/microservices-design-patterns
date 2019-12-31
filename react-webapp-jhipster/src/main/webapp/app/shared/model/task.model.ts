import { Moment } from 'moment';

export interface ITask {
  id?: string;
  name?: string;
  createdByUser?: string;
  createdDate?: Moment;
  lastModifiedByUser?: string;
  lastModifiedDate?: Moment;
}

export const defaultValue: Readonly<ITask> = {};
