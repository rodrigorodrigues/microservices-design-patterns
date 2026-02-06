/**
 * Created by eliasmj on 03/12/2016.
 * Legacy code, very messy
 * TO be deleted ****************************
 */
const router = require('express').Router();
var guard = require('express-jwt-permissions')({
    permissionsProperty: 'authorities'
});
const Q = require('q');

const log = require('../utils/log.message');
const ProductService = require('../services/product.service');
const {Ingredient} = require('../models/ingredient.model');
const {Category} = require('../models/category.model');
const {IngredientRecipeAttributes} = require('../models/ingredient.recipe.attributes.model');
const {Recipe} = require('../models/recipe.model');
const {_} = require('lodash');
const {STATUS} = require('../constants/status.code');
const checkPermissionRoute = require('./checkPermissionRoute');
const UtilService = require('../services/util');

/**
 * @swagger
 *
 * definitions:
 *   Ingredient:
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
 *       categoryName:
 *         type: string
 *         minLength: 1
 *       expiryDate:
 *         type: string
 *         format: date
 *       updateCheckDate:
 *         type: string
 *         format: date
 *       checkedInCartShopping:
 *         type: boolean
 *       tempRecipeLinkIndicator:
 *         type: boolean
 *       attributes:
 *         $ref: '#/definitions/IngredientRecipeAttributes'
 *   IngredientRecipeAttributes:
 *     type: object
 *     required:
 *       - name
 *       - ingredientId
 *       - recipeId
 *     properties:
 *       name:
 *         type: string
 *       labelQuantity:
 *         type: string
 *       quantity:
 *         type: number
 *       itemSelectedForShopping:
 *         type: boolean
 *       ingredientId:
 *         type: integer
 *       recipeId:
 *         type: integer
 *       checkedInCartShopping:
 *         type: boolean
 *       isRecipeLinkedToCategory:
 *         type: boolean
*/

/**
 * @swagger
 * /ingredient:
 *   get:
 *     description: Return Ingredients
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
 *         description: ingredients
 *         schema:
 *           type: array
 *           items:
 *             $ref: '#/definitions/Ingredient'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.get("/ingredient", guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_CREATE'], ['ROLE_INGREDIENT_READ'], ['ROLE_INGREDIENT_SAVE'], ['ROLE_INGREDIENT_DELETE']), async (request, response, next) => {

    const page = parseInt(request.query.page) || 0;
    const size = parseInt(request.query.size) || 10;
    const skip = page * size;

    try {
        // Get total count
        const totalElements = await Ingredient.countDocuments();

        // Get paginated data
        const docs = await Ingredient.find()
            .sort({'name': 1})
            .skip(skip)
            .limit(size);

        // Calculate total pages
        const totalPages = Math.ceil(totalElements / size);

        // Build page response
        const pageResponse = {
            content: docs,
            number: page,
            size: size,
            totalPages: totalPages,
            totalElements: totalElements,
            first: page === 0,
            last: page >= totalPages - 1
        };

        UtilService.handleResponse(response, pageResponse, 200);
    } catch (reason) {
        UtilService.wmHandleError(response, reason);
    }

});

/**
 * @swagger
 * /ingredient/{id}:
 *   get:
 *     description: Return Ingredient by id
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
 *         description: return ingredient by id
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Ingredient'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.get("/ingredient/:id", guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_READ'], ['ROLE_INGREDIENT_SAVE']), (request, res, next) => {

    log.logOnRoutes("ingredient name", request.params.id);

    Ingredient.findOne({_id: request.params.id})
        .then((doc) => {

            UtilService.handleResponse(res, doc, 200);
        }, (reason) => {
            UtilService.wmHandleError(res, reason);
        });

});

/**
 * @swagger
 * /ingredient/recipe/{ingredientId}/{recipeId}:
 *   get:
 *     description: Return Ingredients for Recipe by Ingredient id and Recipe id
 *     produces:
 *      - application/json
 *     parameters:
 *      - name: Authorization
 *        in: header
 *        description: Authorization Header
 *        required: true
 *        type: string
 *      - name: ingredientId
 *        in: path
 *        description: ingredient id
 *        required: true
 *        type: string
 *      - name: recipeId
 *        in: path
 *        description: recipe id
 *        required: true
 *        type: string
 *     responses:
 *       200:
 *         description: return ingredients for Recipe by Ingredient id and Recipe id
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/IngredientRecipeAttributes'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.get("/ingredient/recipe/:ingredientId/:recipeId", guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_READ'], ['ROLE_INGREDIENT_SAVE']), (request, res, next) => {

    log.logOnRoutes("params", request.params.ingredientId);
    log.logOnRoutes("params", request.params.recipeId);

    IngredientRecipeAttributes.findOne({
        ingredientId:request.params.ingredientId,
        recipeId:request.params.recipeId
    })
    .then((doc) => {
        UtilService.handleResponse(res, doc, 200);
    }, (reason) => {
        UtilService.wmHandleError(res, reason);
    });

});

//FIXME deprecated ??
router.get("/ingredient/recipe/:recipeId", guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_READ'], ['ROLE_INGREDIENT_SAVE']), (request, res, next) => {

    log.logOnRoutes("params", request.params.recipeId);

    IngredientRecipeAttributes.find({
        recipeId:request.params.recipeId
    }).then(doc => {

            UtilService.handleResponse(res, doc, 200);
        }, (reason) => {
            UtilService.wmHandleError(res, reason);
        });
});

/**
 * @swagger
 * /ingredient:
 *   post:
 *     description: Create Ingredient
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
 *        description: Create Ingredient
 *        required: true
 *        schema:
 *          $ref: '#/definitions/Ingredient'
 *     responses:
 *       201:
 *         description: create ingredient
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Ingredient'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 */
router.post('/ingredient', guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_CREATE']), (request, res, next) => {

    let recipeId = null;
    if(_.has(request, 'body.ingredientRecipeAttributes.recipeId')) {
        recipeId = request.body.ingredientRecipeAttributes.recipeId;
    }

    //TODO need a stronger test case on it, its breaking too much
    validate(request)
        .then(saveIngredient)
        .then(saveAttribute)
        .then(findCategoryAndAddToIt.bind(null, recipeId))
        .then(ingredient => {
            UtilService.handleResponse(res, ingredient, 201);
        }).catch((reason) => {
            UtilService.wmHandleError(res, reason);
        });

    function saveIngredient(ingredientCommand) {

        let deferred = Q.defer();

        let ingredientRequest = request.body.ingredient;

        let ingredient = new Ingredient({
            name : ingredientRequest.name,
            _creator: ingredientRequest._creator,
            expiryDate: ingredientRequest.expiryDate,
            updateCheckDate: ingredientRequest.updateCheckDate,
            itemSelectedForShopping: ingredientRequest.itemSelectedForShopping,
            checkedInCartShopping: ingredientRequest.checkedInCartShopping
        });

        ingredient.save().then(doc => {

            let result = {
                ingredient: doc,
                ingredientCommand: ingredientCommand
            }

            deferred.resolve(result);

        }).catch(reason => deferred.reject(reason))


        return deferred.promise;
    }

    function validate(request) {

        let deferred = Q.defer();

        let errorResponse = getErrorResponse();

        if(!_.has(request, 'body')) {

            errorResponse.message = "No body found";
            errorResponse.reason = "No body"

            deferred.reject(errorResponse);

        } else if(!_.has(request, "body.ingredient")) {

            errorResponse.message = "no ingredient found";
            errorResponse.reason = "body wrong format";

            deferred.reject(errorResponse);

        } else if(!_.has(request, 'body.ingredient._creator') ) {

            errorResponse.message = "Missing category id";
            errorResponse.reason = "Id missing";

            deferred.reject(errorResponse);

        } else if(!_.has(request, 'body.ingredientRecipeAttributes')) {

            errorResponse.message = "Missing ingredientRecipeAttributes";
            errorResponse.reason = "ingredientRecipeAttributes missing";

            deferred.reject(errorResponse);

        } else {
            deferred.resolve(request.body);
        }

        return deferred.promise;
    }

    function saveAttribute(request) {

        let deferred = Q.defer();

        let ingredientId = request.ingredient._id;
        let attribute = request.ingredientCommand.ingredientRecipeAttributes;

        if(attribute) {

            Recipe.findOne({_id: attribute.recipeId})
                .then((recipe) => {

                    let ingredientRecipeAttributes = getAttribute(attribute, ingredientId, recipe.name);

                    //TODO *** quite old refactor hell
                    ingredientRecipeAttributes.save()
                        .then((doc) => {

                            Ingredient.findOne({_id: ingredientId})
                                .then(ingredient => {

                                    ingredient.attributes.push(doc);

                                    ingredient.save()
                                        .then( () => {

                                            recipe.attributes.push(doc);

                                            recipe.save()
                                                .then(() => {

                                                    deferred.resolve(request.ingredient);

                                                }).catch(reason => deferred.reject(reason));

                                        }).catch(reason => deferred.reject(reason));
                                });

                        }).catch(reason => deferred.reject(reason));
                });

        } else {
            //TODO SHOULD create one, get the functions getAttribute from recipe.route
            deferred.resolve({});
        }

        return deferred.promise;
    }


});

/**
 * @swagger
 * /ingredient:
 *   put:
 *     description: Update Ingredient
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
 *        description: Update Ingredient
 *        required: true
 *        schema:
 *          $ref: '#/definitions/Ingredient'
 *     responses:
 *       200:
 *         description: update ingredient
 *         schema:
 *           type: object
 *           items:
 *             $ref: '#/definitions/Ingredient'
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 *       204:
 *         description: No Content
 *       404:
 *         description: Not Found
 */
router.put('/ingredient', guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_SAVE']), (request, res, next) => {
    // ** Concept status ** use 204 No Content to indicate to the client that
    //... it doesn't need to change its current "document view".
    let ingredientCommand = request.body;

    let recipeId = null;

    //console.log("REQUEST UPDATE LODASH has", _.has(ingredientCommand, 'ingredientRecipeAttributes.recipeId'))

    if(_.has(ingredientCommand, 'ingredientRecipeAttributes.recipeId')) {
        recipeId = ingredientCommand.ingredientRecipeAttributes.recipeId;
    }

    if(ingredientCommand.ingredientRecipeAttributes) {

        updateIngredient(ingredientCommand)
            .then(updateAttributes)
            .then(findCategoryAndAddToIt.bind(null, recipeId))
            .then(doc => {

                UtilService.handleResponse(res, doc, 204);
            })
            .catch(reason => UtilService.wmHandleError(res, reason));

    } else {
         updateIngredient(ingredientCommand)
             .then(resultChain => {
                findCategoryAndAddToIt(recipeId, resultChain.ingredient)
                    .then(doc =>  UtilService.handleResponse(res, doc, 204));
             })
             .catch(reason => UtilService.wmHandleError(res, reason));
    }


    function updateIngredient(ingredientCommand) {

        let deferred = Q.defer();
        let resultChain = {
            ingredient: ingredientCommand.ingredient,
            ingredientRecipeAttributes:  ingredientCommand.ingredientRecipeAttributes,
        }

        Ingredient.findOneAndUpdate({_id: ingredientCommand.ingredient._id}, ingredientCommand.ingredient)
            .then(() => {

                deferred.resolve(resultChain);

            }, (reason) => deferred.reject(reason));

        return deferred.promise;
    }

    function updateAttributes(resultChain) {
        let deferred = Q.defer();

        let attributesRequest = resultChain.ingredientRecipeAttributes;

        if(attributesRequest._id) {

            IngredientRecipeAttributes.findOneAndUpdate({_id: attributesRequest._id}, attributesRequest)
                .then(() => {

                    deferred.resolve(resultChain.ingredient);
                }).catch(reason => deferred.reject(reason));

        } else {

            //Workaround to index unique bug, discriminators
            //name: 'attributes_'+new Date().getTime()
            let ingredientRecipeAttributes = getAttribute(attributesRequest);

            ingredientRecipeAttributes.save()
                .then(() => {
                    deferred.resolve(resultChain.ingredient);
                }).catch(reason => deferred.reject(reason));
        }

        return deferred.promise;
    }


});

router.put('/ingredient/attribute', guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_SAVE']), (request, response, next) => {

    let attributeUpdate = request.body;

    IngredientRecipeAttributes.findOneAndUpdate({_id: attributeUpdate._id}, attributeUpdate)
        .then(doc => {
            UtilService.handleResponse(response, doc, 204);
        }).catch(reason => UtilService.wmHandleError(res, reason));
});

router.put('/ingredient/attribute/many', guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_SAVE']), (request, response, next) => {

    let attributeList = request.body;

    let asyncIteration = attributeList.length;

    if(asyncIteration === 0) {
        handleResponse(response, {}, 204);
    } else {

        attributeList.forEach(attribute => {
            IngredientRecipeAttributes.findOneAndUpdate({_id: attribute._id}, attribute)
                .then(doc => {

                    if(--asyncIteration === 0) {
                        UtilService.handleResponse(response, doc, 204);
                    }

                }).catch(reason => UtilService.wmHandleError(response, reason));
        });
    }

});

/**
 * @swagger
 * /ingredient/{id}:
 *   delete:
 *     description: Delete Ingredient
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
 *         description: Deleted Ingredient
 *       400:
 *         description: Bad Request
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Not Found
 */
router.delete('/ingredient/:id', guard.check(['ROLE_ADMIN'], ['ROLE_INGREDIENT_DELETE']), (request, res, next) => {

    Ingredient.findByIdAndRemove(request.params.id)
        .then((doc) => {
            UtilService.handleResponse(res, doc, 204);
        }, (reason) => {
            UtilService.wmHandleError(res, reason);
        });
});

checkPermissionRoute(router);

function getAttribute(attributesRequest, ingredientId, recipeName) {

    return  new IngredientRecipeAttributes({
        labelQuantity: attributesRequest.labelQuantity,
        quantity: attributesRequest.quantity,
        ingredientId: ingredientId ? ingredientId : attributesRequest.ingredientId,
        recipeId: attributesRequest.recipeId,
        itemSelectedForShopping: attributesRequest.itemSelectedForShopping,
        name: recipeName
    });
}

//Add Ingredient to category, category.ingredients.push
//and add category to Recipe recipe.categories.push
function findCategoryAndAddToIt(recipeId, ingredient) {

    let deferred = Q.defer();

    if(!recipeId) {
        deferred.reject({message: "recipeId not sent"});
        return deferred.promise;
    }

    Category.findOne({_id: ingredient._creator})
        .populate('ingredients')
        .then(cat => {

            let tempIngredient = cat.ingredients.find(ing => ingredient._id.toString() === ing._id.toString())

            if(tempIngredient === undefined) {
                cat.ingredients.push(ingredient);
            }

            //TODO refactor, maybe change to category post
            cat.save()
                .then( doc => {

                    //need to add to recipe cat array
                    Recipe.findOne({_id: recipeId}).then(recipe => {

                        if(recipe) {

                            addItem(recipe.categories, cat);

                            recipe.save().then(() => {

                                deferred.resolve(ingredient);

                            }).catch(reason => deferred.reject(reason));
                        } else {

                            deferred.reject({message: "Not recipe found to add the category"})
                        }

                    }).catch(reason => deferred.reject(reason));

                }).catch(reason => deferred.reject(reason));

        }).catch(reason => {
             deferred.reject(reason)
        });

    return deferred.promise;
}

function getErrorResponse() {
    var errorResponse = {
        message : null,
        name: null,
        errors: null
    };

    return errorResponse;
}

//TODO move to utils
//Should return a list?
function addItem(list, item) {

    let tempItem = list.find(itemIn => {

        return getValue(itemIn) === getValue(item);
    });

    if(!tempItem) {
        list.push(item);
        return true;
    } else {
        return false;
    }
}

function getValue(value) {

    return value['_id'] !== undefined ? value._id.toString() : value.toString();

}



module.exports = router;