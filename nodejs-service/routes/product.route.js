const log = require('../utils/log.message');
const ProductService = require('../services/product.service');
const router = require('express').Router();
const responseHandlerService = require('../services/response.handler.service');
const { STATUS } = require('../constants/status.code');

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

router.put("/product", updateProductRoute);
router.post("/product", saveProductRoute);

module.exports = router;