const router = require('express').Router();
const ShoppingListService = require('../services/shopping.list.service');
const responseHandlerService = require('../services/response.handler.service');
const { STATUS } = require('../constants/status.code');

function getOne(request, response) {
    ShoppingListService
        .getOne(request.params.id)
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.GET_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}
function get(request, response) {
    ShoppingListService
        .get()
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.GET_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}
function save(request, response) {
    ShoppingListService
        .save(request.body)
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.CREATE_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}
function update(request, response) {
    ShoppingListService
        .update(request.body)
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.UPDATE_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}
function updateItem(request, response) {
    ShoppingListService
        .updateItem(request.body)
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.UPDATE_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}

router.get('/shoppingList/:id', getOne);
router.get('/shoppingList', get);
router.post('/shoppingList', save);
router.put('/shoppingList', update);
router.put('/shoppingList/item', updateItem);

module.exports = router;