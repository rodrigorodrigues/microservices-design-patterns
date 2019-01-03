import { errorMessage } from './Util';

describe('Util', () => {
    it('should return object when pass an error text', () => {
        expect(errorMessage('Error text'))
            .toEqual({message: {error: 'Error text'}, type: 'danger'})
    });
    it('should return object when pass an error object', () => {
        expect(errorMessage({error: 'Error Object'}))
            .toEqual({message: {error: 'Error Object'}, type: 'danger'})
    });
    it('should return object when pass an error object 2', () => {
        expect(errorMessage({message: 'Error Object 2'}))
            .toEqual({message: {error: 'Error Object 2'}, type: 'danger'})
    });
})