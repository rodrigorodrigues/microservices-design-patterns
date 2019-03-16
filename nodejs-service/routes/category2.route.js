const router = require('express').Router();
var guard = require('express-jwt-permissions')({
    permissionsProperty: 'authorities'
});
const responseHandlerService = require('../services/response.handler.service');
const CategoryService = require('../services/category.service');
const {STATUS} = require('../constants/status.code');
const checkPermissionRoute = require('./checkPermissionRoute');

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

router.get('/category', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_CREATE'], ['ROLE_CATEGORY_READ'], ['ROLE_CATEGORY_SAVE'], ['ROLE_CATEGORY_DELETE']), getCategoryRoute);
router.post('/category', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_CREATE']), save);
router.put('/category', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_SAVE']), update);

checkPermissionRoute(router);

module.exports = router;