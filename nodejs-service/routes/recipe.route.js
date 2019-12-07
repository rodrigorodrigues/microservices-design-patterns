/**
 * Created by eliasmj on 26/11/2016.
 * Legacy code, very messy
 * TO be deleted ****************************
 */
const express = require('express');
var guard = require('express-jwt-permissions')({
    permissionsProperty: 'authorities'
});
const router = express.Router();

const array = require('lodash/array');

const log = require('../utils/log.message');

const {Recipe} = require('../models/recipe.model');

const {Category} = require('../models/category.model');

const {Ingredient} = require('../models/ingredient.model');

const {IngredientRecipeAttributes} = require('../models/ingredient.recipe.attributes.model');

const Q = require('q');

const utility = require('../utils/utility');

const checkPermissionRoute = require('./checkPermissionRoute');

router.get("/recipe", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_CREATE'], ['ROLE_RECIPE_READ'], ['ROLE_RECIPE_SAVE'], ['ROLE_RECIPE_DELETE']), (req, res) => {

    Recipe
        .find()
        .sort({name: 1})
        .then((doc) => {
            utility.handleResponse(res, doc, 200);
        }, (reason) => {
            utility.wmHandleError(res, reason);
        });
});

router.get("/recipe/week", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_READ']), (req, res) => {

    Recipe
        .find({isInMenuWeek: true})
        .sort({name: 1})
        .then(doc => {

            utility.handleResponse(res,  utility.sortList(doc), 200);
        }, (reason) => {
            utility. wmHandleError(res, reason);
        });
});


router.get("/recipe/:id", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_READ'], ['ROLE_RECIPE_SAVE']), (req, res) => {

    log.logOnRoutes("Recipe name", req.params.id);

    Recipe.findOne({_id: req.params.id})
        .then((doc) => {
            utility.handleResponse(res, doc, 200);
        }, (reason) => {
            utility.wmHandleError(res, reason);
        });

});


router.get("/recipe/category/:id", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_READ'], ['ROLE_RECIPE_SAVE']), (req, response) => {

    Recipe
        .findOne({_id: req.params.id})
        .populate('categories')
        .then((populated) => {

            if (!populated) {
                return utility.handleResponse(response, populated, 200);
            }

            const options = {
                path: 'categories.ingredients',
                model: 'Ingredient',
                sort: { name: 1 }
            };

            /**
             * TODO need to write a test case for it
             * Make sure after check and unchecked it will return the corretc list
             *
             * Also test together the caegory resource /category/check/:recipeId that is direct
             * related to this rule
             * TODO
             */

            Recipe.populate(populated,options)
                .then(deepPopulated => {
                    //console.log("deep docSaved", deepPopulated.categories);

                    //I need to check if there is an attribute flag true
                    // for each ingredients and return filtered based on it.
                    IngredientRecipeAttributes
                        .find({recipeId: deepPopulated._id}).where('isRecipeLinkedToCategory').equals(true)
                        .then(attributes => {

                            deepPopulated.categories.forEach(category => {

                                //console.log("Ingredient SIZE=", category.ingredients.length)

                                category.ingredients = category.ingredients.filter(ing => {

                                    let found = attributes
                                        .find(atrr  => atrr.ingredientId.toString() === ing._id.toString());


                                    //console.log(ing.name + " FOUND=", found !== undefined)
                                    return found !== undefined;

                                });

                               // console.log("Filtered SIZE =", filteredIng.length)
                            });

                            //filtering category with ingredients == 0
                            deepPopulated.categories = deepPopulated.categories.filter(cat => cat.ingredients.length > 0);

                            utility.handleResponse(response, deepPopulated, 200);
                        })


                }).catch( (reason) => {
                    utility.wmHandleError(response, reason);
                });

        }, (reason) => {
            utility.wmHandleError(response, reason);
        });
});

router.get("/recipe/category/currentAttribute/:id", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_READ'], ['ROLE_RECIPE_SAVE']), (req, response) => {

    Recipe
        .findOne({_id: req.params.id})
        .populate('categories')
        .then(populated => {

            const options = {
                path: 'categories.ingredients',
                model: 'Ingredient'
            };

            Recipe.populate(populated,options)
                .then(recipeDeepPopulated => {

                    const options2 = {
                        path: 'categories.ingredients.attributes',
                        model: 'IngredientRecipeAttributes',
                        match: {recipeId : req.params.id, isRecipeLinkedToCategory: true}
                    };

                    Recipe.populate(recipeDeepPopulated, options2)
                        .then(level3 => {

                            if (level3 && level3.categories) {

                                //console.log("Before filter", level3.categories)

                                level3.categories.forEach(cat => {

                                    cat.ingredients = cat.ingredients.filter(ing => ing.attributes.length > 0);
                                });

                                level3.categories = level3.categories.filter(cat => cat.ingredients.length > 0);

                            }

                            // console.log("deep docSaved", level3.categories);
                            utility.handleResponse(response, level3, 200);
                            
                        })

                }).catch( (reason) => {
                    utility.wmHandleError(response, reason);
                });

        }, (reason) => {
            utility.wmHandleError(response, reason);
        });
});


router.post('/recipe', guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_CREATE']), (request, res) => {

    let recipeCommand = request.body;

    const recipe = new Recipe({
        name : recipeCommand.name,
        weekDay: recipeCommand.weekDay,
        isInMenuWeek: recipeCommand.isInMenuWeek,
        mainMealValue: recipeCommand.mainMealValue,
        menus: recipeCommand.menus,
    });

    recipe.save()
        .then((doc) => {
            utility.handleResponse(res, doc, 201);
        }, (reason) => {
            utility.wmHandleError(res, reason);
        });
});

router.put('/recipe', guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_SAVE']), (request, res) => {
    // ** Concept status ** use 204 No Content to indicate to the client that
    //... it doesn't need to change its current "document view".

    let recipeCommand = request.body;

    Recipe.findOne({_id: recipeCommand._id})
        .then((docUpdated) => {

            docUpdated.name = recipeCommand.name;
            docUpdated.weekDay = recipeCommand.weekDay;
            docUpdated.mainMealValue = recipeCommand.mainMealValue;
            docUpdated.description = recipeCommand.description;
            docUpdated.isInMenuWeek = recipeCommand.isInMenuWeek;

            //TODO Need test for this case
            //**If isInMenuWeek = true set, should set all for true otherwise to false**
            let itemSelectedForShoppingForTrue = docUpdated.isInMenuWeek;
            setItemSelectedForShopping(docUpdated._id, itemSelectedForShoppingForTrue)
                .then(() => {
                    updateRecipe();
                });

            function updateRecipe() {
                docUpdated.save()
                    .then( () => {
                        utility.handleResponse(res, docUpdated, 204);
                    }, (reason) => utility.wmHandleError(res, reason));
            }


        }, (reason) => utility.wmHandleError(res, reason));
});

/**
 * only one category request
 * there is cat => delete, push
 * there is not => push

 * there is Attr => push
 * there is not => create, push

 save ingredient and recipe arrays
 */
router.put("/recipe/ingredient", guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_SAVE']), (request, response) => {

    //only one ingredient
    // check false or true(checkbox)
    //false need to remove from cat
    //true add to cat

    Recipe
        .findOne({_id: request.body._id})
        .populate('categories')
        .then(recipe => {

            //on Ionic app its always sending one array, after each action check it sends only one ingredient
            let ingredient = request.body.ingredient;

            //TODO for the future, if tempRecipeLinkIndicator=false does not need create attribute
            getAttribute(recipe, ingredient)
                .then(saveInIngredient.bind(null, ingredient._id))
                .then(attribute => {

                    //console.log('Begin', attribute.name, attribute.isRecipeLinkedToCategory);

                    //TODO review recipe.attributes Im not using it
                    utility.addItem(recipe.attributes, attribute);

                    let tempCategory = utility.findAny(recipe.categories, ingredient._creator);

                    if(!tempCategory) {
                        addCategoryToRecipe(recipe, ingredient._creator)
                            .then(carryOn);
                    } else {
                        //if there is cat only carry on
                        carryOn(tempCategory);
                    }

                    function carryOn(category) {

                        if(attribute.isRecipeLinkedToCategory) {

                            //console.log('BEFORE  add', category.ingredients.length)

                            utility.addItem(category.ingredients, ingredient);

                            //console.log('AFTER added '+added , category.ingredients.length)

                        } else {
                            //console.log(" flag was false unchecked")

                        }

                        category.save()
                            .then(() => {

                                recipe.save().then(() => {
                                    utility.handleResponse(response, {}, 204);

                                }).catch(reason => utility.wmHandleError(response, reason));
                            }).catch(reason => utility.wmHandleError(response, reason));
                    }

                });

        }, (reason) => {
            utility.wmHandleError(response, reason);
        });
});

router.delete('/recipe', guard.check(['ROLE_ADMIN'], ['ROLE_RECIPE_DELETE']), (req, res) => {

    Recipe.findByIdAndRemove(req.body._id)
        .then((doc) => {
            utility.handleResponse(res, doc, 204);
        }, (reason) => {
            utility.wmHandleError(res, reason);
        });
});

checkPermissionRoute(router);

//recipe does not have category added yet
function addCategoryToRecipe(recipe, id) {
    let deferred = Q.defer();

    Category.findOne({_id: id}).then(category => {

        utility.addItem(recipe.categories, category);

        recipe.save().then(() => {

            //console.log("added to rec", recipe.categories.length)

            //to add in the recipe reference
            deferred.resolve(utility.findAny(recipe.categories, category));

        }).catch(reason => deferred.reject(reason));


    }).catch(reason => deferred.reject(reason));

    return deferred.promise;
}

function getAttribute(recipe, ingredient) {

    let deferred = Q.defer();

    let ingredientId = ingredient._id;

    let isRecipeLinkedToCategory = ingredient.tempRecipeLinkIndicator;

    IngredientRecipeAttributes
        .findOne({recipeId: recipe._id, ingredientId: ingredientId})
        .then(attr => {

            if(!attr) {

                let attribute = new IngredientRecipeAttributes({
                    recipeId: recipe._id,
                    ingredientId: ingredientId,
                    name: recipe.name,
                    isRecipeLinkedToCategory: isRecipeLinkedToCategory
                });

                attribute.save().then(saved => {

                    deferred.resolve(saved);

                }).catch(reason => deferred.reject(reason));


            } else {

                attr.isRecipeLinkedToCategory = isRecipeLinkedToCategory;

                attr.save().then(() => {
                    deferred.resolve(attr);
                }).catch(reason => deferred.reject(reason));
            }

        }).catch(reason => deferred.reject(reason));


    return deferred.promise;
}

function saveInIngredient(ingredientId, attribute) {
    let deferred = Q.defer();

    //console.log("Save ingredi params " + ingredientId, attribute)

    Ingredient.findOne({_id: ingredientId})
        .then(ingredient => {

            utility.addItem(ingredient.attributes, attribute);
//            console.log("INgredient " + ingredient.name)

            ingredient.save()
                .then(() => {
                    deferred.resolve(attribute);
                }).catch(reason => deferred.reject(reason));

        }).catch(reason => deferred.reject(reason));

    return deferred.promise;
}


function setItemSelectedForShopping(recipeId, flag) {
    let deferred = Q.defer();

    let listIdsFailed = [];

    IngredientRecipeAttributes.find({itemSelectedForShopping: !flag, recipeId: recipeId})
        .then(attrs => {

            attrs.forEach(attr => {
                attr.itemSelectedForShopping = flag;
                //not treating if failed .save(), it will do for now
                attr.save()
                    .catch(reason => {
                        log.logOnRoutes("Failed to save attribute flag", attr._id);
                        listIdsFailed.push(attr._id);
                    });
            });

            deferred.resolve(listIdsFailed);

        }).catch(reason =>{ deferred.reject(reason) });

    return deferred.promise;
}


module.exports = router;