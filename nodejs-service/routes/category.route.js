/**
 * Created by eliasmj on 03/12/2016.
 * Legacy code, very messy
 * TO be deleted ****************************
 */
const router = require('express').Router();
var guard = require('express-jwt-permissions')({
    permissionsProperty: 'authorities'
});
const log = require('../utils/log.message');

const { Category } = require('../models/category.model');
const { Recipe } = require('../models/recipe.model');
const { IngredientRecipeAttributes } = require('../models/ingredient.recipe.attributes.model');

//***************************************** 

const categoryFile = require('../db_backup/categories.json')
const ingredFile = require('../db_backup/ingredients.json')
const recipeFile = require('../db_backup/recipes.json')
const attrsFile = require('../db_backup/attributes.json')
const { Recipe2 } = require('../models/recipe2.model');
const restoreBackup = require("../services/restoreBackup");

const checkPermissionRoute = require('./checkPermissionRoute');

const UtilService = require('../services/util');

router.get("/category/migration/cat/23", (request, response, next) => {
    Category.remove({}).then(() => {
        const cats = JSON.parse(JSON.stringify(categoryFile))
        const ingreds = JSON.parse(JSON.stringify(ingredFile))

        cats.forEach(category => {
            const products = getAllProducst(category._id.$oid);
            console.log(category.name, products)

            const categoryModel = new Category({
                name: category.name,
                insertDate: new Date(),
                products: products
            });
            categoryModel.save()
        })

        function getAllProducst(id) {
            let res = [];
            ingreds.forEach(ing => {
                if (ing._creator.$oid == id) {
                    const obj = {
                        name: ing.name,
                        insertDate: new Date()
                    }
                    res.push(obj)
                }
            })
            return res
        }

        UtilService.handleResponse(response, "ok *** *** **", 200)
    })
});

router.get("/category", guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_CREATE'], ['ROLE_CATEGORY_READ'], ['ROLE_CATEGORY_SAVE'], ['ROLE_CATEGORY_DELETE']), async (request, response, next) => {

    const page = parseInt(request.query.page) || 0;
    const size = parseInt(request.query.size) || 10;
    const skip = page * size;

    try {
        // Get total count
        const totalElements = await Category.countDocuments();

        // Get paginated data
        const categories = await Category.find()
            .populate('ingredients')
            .sort({ 'name': 1 })
            .skip(skip)
            .limit(size);

        // Calculate total pages
        const totalPages = Math.ceil(totalElements / size);

        // Build page response
        const pageResponse = {
            content: categories,
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

// ###### improved above 

router.get("/category/check/:recipeId", guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_READ']), (request, response) => {

    //TODO write test for different/strong test case because it's really hard to improve this function at the moment
    //read /recipe/category comments
    Category.find()
        .populate('ingredients')
        .sort({ 'name': 1 })
        .then(allCategories => {
            linkRecipeToIngredients(allCategories);

        }, (reason) => {
            UtilService.wmHandleError(response, reason);
        });

    function linkRecipeToIngredients(allCategories) {

        let countCat = allCategories.length;

        if (countCat === 0) {
            UtilService.handleResponse(response, allCategories, 200);
        }

        Recipe.findOne({ _id: request.params.recipeId })
            .populate('categories')
            .sort({ 'name': 1 })
            .then(recipe => {

                let recipeId = recipe._id;

                allCategories.forEach(catFromAll => {

                    let countIngredient = catFromAll.ingredients.length;

                    //console.log("Counts in loop ", countCat, countIngredient)

                    if (countIngredient === 0) {

                        if (--countCat === 0) {
                            UtilService.handleResponse(response, allCategories, 200);
                        }

                    } else {

                        catFromAll.ingredients.forEach(ingToBeSend => {
                            //console.log("CAt Name", catFromAll.name, ingToBeSend.name)
                            //reset always because virtual does not work!
                            ingToBeSend.tempRecipeLinkIndicator = false;

                            let ingredientId = ingToBeSend._id;

                            IngredientRecipeAttributes.findOne({ recipeId, ingredientId }).then(attr => {

                                if (attr) {
                                    ingToBeSend.tempRecipeLinkIndicator = attr.isRecipeLinkedToCategory;

                                    //console.log("attr", attr.name, recipe.name)

                                } else {
                                    ingToBeSend.tempRecipeLinkIndicator = false
                                }

                                //console.log("Ingredient " + ingToBeSend.name, ingToBeSend.tempRecipeLinkIndicator)

                                if (--countIngredient <= 0) {

                                    //console.log("Going!", countIngredient, countCat)
                                    if (--countCat === 0) {
                                        UtilService.handleResponse(response, allCategories, 200);
                                    }
                                }

                            });
                        });
                    }

                });

                // console.log("****** CAT UPDATED", categories)
            }).catch(reason => UtilService.wmHandleError(response, reason));
    }
});

router.get("/category/week/shopping", guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_READ']), (request, response, next) => {

    Category.find()
        .sort({ name: 1 })
        .populate({
            path: 'ingredients',
            options: { sort: { name: 1 } }
        })
        .then((docs) => {
            if (!docs) {
                return UtilService.handleResponse(response, docs, 200);
            }

            const options = {
                path: 'ingredients.attributes',
                model: 'IngredientRecipeAttributes',
                sort: { name: 1 },
                match: { itemSelectedForShopping: true, isRecipeLinkedToCategory: true }
            };

            Category.populate(docs, options).then(deep => {
                let categories = deep.filter(category => {

                    if (category.ingredients) {

                        //filter based on the query above, if there didn't match any attribute the array is 0
                        let ingredients = category.ingredients.filter(ingredient => ingredient.attributes.length > 0);

                        category.ingredients = ingredients;

                        return category.ingredients.length > 0;

                    }

                });

                //filter by recipe in the week menu
                // categories = categories.filter(cat => {
                //     return cat.recipes.length > 0;
                // });
                if (categories.length > 0) {
                    console.log(`categories for week shopping: ${categories}`);

                    UtilService.handleResponse(response, categories, 200);
                } else {
                    UtilService.handleResponse(response, docs, 200);
                }
            }).catch(reason => UtilService.wmHandleError(response, reason));
        }, (reason) => {
            UtilService.wmHandleError(response, reason);
        });
});

router.get("/category/:id", guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_READ'], ['ROLE_CATEGORY_SAVE']), (req, res, next) => {

    log.logOnRoutes("category name", req.params.id);

    Category.findOne({ _id: req.params.id })
        .then((doc) => {
            UtilService.handleResponse(res, doc, 200);
        }, (reason) => {
            UtilService.wmHandleError(res, reason);
        });

});

router.post('/category', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_CREATE']), (req, res, next) => {

    const request = req.body;

    const category = new Category({
        name: request.name
    });

    if (request.recipeId) {

        let tempRecipe = category.recipes.find(rec => rec._id.toString() === request.recipeId);

        if (!tempRecipe) {

            Recipe.findOne({ _id: request.recipeId })
                .then(recipe => {

                    category.recipes.push(recipe);

                    saveCategory();

                });
        } else {
            saveCategory();
        }

    } else {
        saveCategory();
    }

    function saveCategory() {
        category.save()
            .then((doc) => {
                UtilService.handleResponse(res, doc, 201);
            }, (reason) => {
                UtilService.wmHandleError(res, reason);
            });
    }

});

router.put('/category', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_SAVE']), (req, res, next) => {
    // ** Concept status ** use 204 No Content to indicate to the client that
    //... it doesn't need to change its current "document view".

    let request = req.body;

    Category.findOne({ _id: req.body._id })
        .populate('recipes')
        .then(category => {

            category.name = req.body.name;

            if (request.recipeId) {

                let tempRecipe = category.recipes.find(rec => rec._id.toString() === request.recipeId);

                if (!tempRecipe) {

                    //TODO repeated code /post
                    Recipe.findOne({ _id: request.recipeId })
                        .then(recipe => {

                            category.recipes.push(recipe);

                            category.save()
                                .then(() => UtilService.handleResponse(res, category, 204))

                        });
                } else {
                    category.save()
                        .then(() => UtilService.handleResponse(res, category, 204))
                }

            } else {
                category.save()
                    .then(() => UtilService.handleResponse(res, category, 204))
            }

        }, (reason) => {
            UtilService.wmHandleError(res, reason);
        });
});

router.delete('/category/:id', guard.check(['ROLE_ADMIN'], ['ROLE_CATEGORY_DELETE']), (req, res, next) => {
    log.logOnRoutes("category id", req.params.id);

    Category.findByIdAndRemove(req.params.id)
        .then((doc) => {
            UtilService.handleResponse(res, doc, 204);
        }, (reason) => {
            UtilService.wmHandleError(res, reason);
        });
});

checkPermissionRoute(router);

module.exports = router;