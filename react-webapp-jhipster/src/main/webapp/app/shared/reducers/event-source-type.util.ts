import { AxiosPromise } from 'axios';
export interface IPayload<T> {
  type: string;
  payload: AxiosPromise<T>;
  meta?: any;
}
export declare type IPayloadResult<T> = (dispatch: any) => IPayload<T> | Promise<IPayload<T>>;
export declare type ICrudGetAllEventSourceAction<T> = (page?: number, size?: number, sort?: string) => IPayload<T> | IPayloadResult<T>;
