const router = require('express').Router();
var guard = require('express-jwt-permissions')({
    permissionsProperty: 'authorities'
});
const responseHandlerService = require('../services/response.handler.service');
const RecipeService = require('../services/recipe.service');
const { STATUS } = require('../constants/status.code');
const checkPermissionRoute = require('./checkPermissionRoute');

function get(request, response) {
    RecipeService
        .get()
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.GET_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}
function getOne(request, response) {
    RecipeService
        .getOne(request.params.id)
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.GET_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}
function save(request, response) {
    RecipeService
        .save(request.body)
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.CREATE_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}
function update(request, response) {
    RecipeService
        .update(request.body)
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.UPDATE_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}

router.get("/recipe", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_CREATE'], ['ROLE_RECIPE_READ'], ['ROLE_RECIPE_SAVE'], ['ROLE_RECIPE_DELETE']), get);
router.get("/recipe/:id", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_READ'], ['ROLE_RECIPE_SAVE']), getOne);
router.post("/recipe", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_CREATE']), save);
router.put("/recipe", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_SAVE']), update);

checkPermissionRoute(router);

module.exports = router;