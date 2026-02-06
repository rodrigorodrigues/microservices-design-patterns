const router = require('express').Router();
var guard = require('express-jwt-permissions')({
    permissionsProperty: 'authorities'
});
const responseHandlerService = require('../services/response.handler.service');
const RecipeService = require('../services/recipe.service');
const { STATUS } = require('../constants/status.code');
const checkPermissionRoute = require('./checkPermissionRoute');

function get(request, response) {
    const page = parseInt(request.query.page) || 0;
    const size = parseInt(request.query.size) || 10;

    RecipeService
        .get(page, size)
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
function deleteById(request, response) {
    RecipeService
        .deleteById(request.params.id)
        .then(doc => responseHandlerService.send(response, { doc, status: STATUS.GET_CODE }))
        .catch(reason => responseHandlerService.error(response, reason));
}

/**
 * @swagger
 * /v2/recipe:
 *   get:
 *     description: Return Recipies
 *     produces:
 *      - application/json
 *     parameters:
 *      - name: Authorization
 *        in: header
 *        description: Authorization Header
 *        required: true
 *        type: string
 *     responses:
 *       200:
 *         description: recipies
 *         schema:
 *           type: array
 *           items:
 *             $ref: '#/definitions/Recipe'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.get("/recipe", guard.check([['ROLE_ADMIN'], ['ROLE_RECIPE_CREATE'], ['ROLE_RECIPE_READ'], ['ROLE_RECIPE_SAVE'], ['ROLE_RECIPE_DELETE'], ['SCOPE_openid']]), get);

/**
 * @swagger
 * /v2/recipe/{id}:
 *   get:
 *     description: Return Recipe by id
 *     produces:
 *      - application/json
 *     parameters:
 *      - name: Authorization
 *        in: header
 *        description: Authorization Header
 *        required: true
 *        type: string
 *      - name: id
 *        in: path
 *        description: id
 *        required: true
 *        type: string
 *     responses:
 *       200:
 *         description: return recipe by id
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Recipe'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.get("/recipe/:id", guard.check([['ROLE_ADMIN'], ['ROLE_RECIPE_READ'], ['ROLE_RECIPE_SAVE']]), getOne);

/**
 * @swagger
 * /v2/recipe:
 *   post:
 *     description: Create Recipe
 *     produces:
 *      - application/json
 *     parameters:
 *      - name: Authorization
 *        in: header
 *        description: Authorization Header
 *        required: true
 *        type: string
 *      - name: RecipeDto
 *        in: body
 *        description: Create Recipe
 *        required: true
 *        schema:
 *          $ref: '#/definitions/Recipe'
 *     responses:
 *       201:
 *         description: create recipe
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Recipe'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.post("/recipe", guard.check([['ROLE_ADMIN'], ['ROLE_RECIPE_CREATE']]), save);

/**
 * @swagger
 * /v2/recipe:
 *   put:
 *     description: Update Recipe
 *     produces:
 *      - application/json
 *     parameters:
 *      - name: Authorization
 *        in: header
 *        description: Authorization Header
 *        required: true
 *        type: string
 *      - name: RecipeDto
 *        in: body
 *        description: Update Recipe
 *        required: true
 *        schema:
 *          $ref: '#/definitions/Recipe'
 *     responses:
 *       200:
 *         description: update recipe
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Recipe'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 *       204:
 *         description: No Content
 *       404:
 *         description: Not Found
 */
router.put("/recipe", guard.check([['ROLE_ADMIN'], ['ROLE_RECIPE_SAVE']]), update);

/**
 * @swagger
 * /v2/recipe/{id}:
 *   delete:
 *     description: Delete Recipe
 *     produces:
 *      - application/json
 *     parameters:
 *      - name: Authorization
 *        in: header
 *        description: Authorization Header
 *        required: true
 *        type: string
 *      - name: id
 *        in: path
 *        description: id
 *        required: true
 *        type: string
 *     responses:
 *       204:
 *         description: Deleted Recipe
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Not Found
 */
router.delete('/recipe/:id', guard.check([['ROLE_ADMIN'], ['ROLE_RECIPE_DELETE']]), deleteById);

checkPermissionRoute(router);

module.exports = router;