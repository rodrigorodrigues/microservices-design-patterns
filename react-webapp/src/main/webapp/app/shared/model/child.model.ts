import { Moment } from 'moment';
import { IPerson } from 'app/shared/model/person.model';

export interface IChild {
  id?: string;
  name?: string;
  dateOfBirth?: Moment;
  person?: IPerson;
}

export const defaultValue: Readonly<IChild> = {};
