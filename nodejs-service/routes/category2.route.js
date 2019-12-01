const router = require('express').Router();
var guard = require('express-jwt-permissions')({
    permissionsProperty: 'authorities'
});
const responseHandlerService = require('../services/response.handler.service');
const CategoryService = require('../services/category.service');
const {STATUS} = require('../constants/status.code');
const checkPermissionRoute = require('./checkPermissionRoute');

function deleteCategoryByIdRoute(request, response) {
    CategoryService
        .deleteById(request.params.id)
        .then(doc => responseHandlerService.send(response, {doc, status: STATUS.GET_CODE}))
        .catch(reason => responseHandlerService.error(response, reason));
}
function getCategoryByIdRoute(request, response) {
    CategoryService
        .getById(request.params.id)
        .then(doc => responseHandlerService.send(response, {doc, status: STATUS.GET_CODE}))
        .catch(reason => responseHandlerService.error(response, reason));
}
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

/**
 * @swagger
 *
 * definitions:
 *   Category:
 *     type: object
 *     required:
 *       - name
 *       - insertDate
 *     properties:
 *       name:
 *         type: string
 *       insertDate:
 *         type: string
 *         format: date
 *       products:
 *         $ref: '#/definitions/Product'
 *   Product:
 *     type: object
 *     required:
 *       - name
 *       - insertDate
 *     properties:
 *       name:
 *         type: string
 *       insertDate:
 *         type: string
 *         format: date
 *       completed:
 *         type: boolean
 *       quantity:
 *         type: integer
 *         default: 1
 *   Recipe:
 *     type: object
 *     required:
 *       - name
 *       - insertDate
 *     properties:
 *       name:
 *         type: string
 *       insertDate:
 *         type: string
 *         format: date
 *       updateDate:
 *         type: string
 *         format: date
 *       categories:
 *         type: array
 *         items:
 *           $ref: '#/definitions/Category'
 */

/**
 * @swagger
 * /v2/category:
 *   get:
 *     description: Return Categories
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
 *         description: categories
 *         schema:
 *           type: array
 *           items:
 *             $ref: '#/definitions/Category'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.get('/category', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_CREATE'], ['ROLE_CATEGORY_READ'], ['ROLE_CATEGORY_SAVE'], ['ROLE_CATEGORY_DELETE']), getCategoryRoute);

/**
 * @swagger
 * /v2/category/{id}:
 *   get:
 *     description: Return Category by id
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
 *         description: return category by id
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Category'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.get("/category/:id", guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_READ'], ['ROLE_CATEGORY_SAVE']), getCategoryByIdRoute);

/**
 * @swagger
 * /v2/category:
 *   post:
 *     description: Create Category
 *     produces:
 *      - application/json
 *     parameters:
 *      - name: Authorization
 *        in: header
 *        description: Authorization Header
 *        required: true
 *        type: string
 *      - name: CategoryDto
 *        in: body
 *        description: Create Category
 *        required: true
 *        schema:
 *          $ref: '#/definitions/Category'
 *     responses:
 *       201:
 *         description: create category
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Category'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.post('/category', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_CREATE']), save);

/**
 * @swagger
 * /v2/category:
 *   put:
 *     description: Update Category
 *     produces:
 *      - application/json
 *     parameters:
 *      - name: Authorization
 *        in: header
 *        description: Authorization Header
 *        required: true
 *        type: string
 *      - name: CategoryDto
 *        in: body
 *        description: Update Category
 *        required: true
 *        schema:
 *          $ref: '#/definitions/Category'
 *     responses:
 *       200:
 *         description: update category
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Category'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 *       204:
 *         description: No Content
 *       404:
 *         description: Not Found
 */
router.put('/category', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_SAVE']), update);

/**
 * @swagger
 * /v2/category/{id}:
 *   delete:
 *     description: Delete Category
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
 *         description: Deleted Category
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Not Found
 */
router.delete('/category/:id', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_DELETE']), deleteCategoryByIdRoute);

checkPermissionRoute(router);

module.exports = router;