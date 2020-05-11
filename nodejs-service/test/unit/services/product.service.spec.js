const ProductService = require('../../../services/product.service');
const {VALIDATION} = require('../../../constants/message.constant');
const expect = require('expect');

describe('Product service', () => {
    it('should return a error message when try to save', () => {
        return ProductService.save({name:'bla'})
            .catch( reason => {
                expect(reason.message).toEqual(VALIDATION.INTERNAL_REQUIRE_CAT);
            });
    })
});

