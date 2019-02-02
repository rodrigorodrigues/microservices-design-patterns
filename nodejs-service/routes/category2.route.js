const router = require('express').Router();
const responseHandlerService = require('../services/response.handler.service');
const CategoryService = require('../services/category.service');
const {STATUS} = require('../constants/status.code');

function getCategoryRoute(request, response) {
    CategoryService
        .get()
        .then(doc => responseHandlerService.send(response, {doc, status: STATUS.GET_CODE}))
        .catch(reason => responseHandlerService.error(response, reason));
}
function save(request, response) {
    CategoryService
        .save(request.body)
        .then(doc => responseHandlerService.send(response, {doc, status: STATUS.CREATE_CODE}))
        .catch(reason => responseHandlerService.error(response, reason));
}
function update(request, response) {
    CategoryService
        .update(request.body)
        .then(() => responseHandlerService.send(response, {status: STATUS.UPDATE_CODE}))
        .catch(reason => responseHandlerService.error(response, reason));
}

router.get('/category', getCategoryRoute);
router.post('/category', save);
router.put('/category', update);

module.exports = router;