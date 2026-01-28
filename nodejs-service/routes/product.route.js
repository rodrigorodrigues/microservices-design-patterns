const log = require('../utils/log.message');
const ProductService = require('../services/product.service');
const router = require('express').Router();
var guard = require('express-jwt-permissions')({
    permissionsProperty: 'authorities'
});
const responseHandlerService = require('../services/response.handler.service');
const { STATUS } = require('../constants/status.code');
const checkPermissionRoute = require('./checkPermissionRoute');

function updateProductRoute(request, response) {
    ProductService
        .update(request.body)
        .then(() => responseHandlerService.send(response, { status: STATUS.UPDATE_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}

function saveProductRoute(request, response) {
    ProductService
        .save(request.body)
        .then(doc => responseHandlerService.send(response, {doc, status: STATUS.CREATE_CODE}))
        .catch(reason => responseHandlerService.error(response, reason));
}

router.put("/product", guard.check([['ROLE_ADMIN'], ['ROLE_PRODUCT_SAVE']['ROLE_ADMIN'], ['ROLE_PRODUCT_SAVE']]), updateProductRoute);
router.post("/product", guard.check([['ROLE_ADMIN'], ['ROLE_ADMIN'], ['ROLE_PRODUCT_CREATE']]), saveProductRoute);

checkPermissionRoute(router);

module.exports = router;