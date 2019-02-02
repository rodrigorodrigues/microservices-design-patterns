const { Recipe2 } = require('../models/recipe2.model');
const CustomValidation = require('../services/custom.validation');
const UtilService = require('./util')

const RecipeService = () => {
    return {
        save(recipePayload) {
            const recipeModel = new Recipe2({
                name: recipePayload.name,
                insertDate: new Date(),
                categories: recipePayload.categories
            });
            return recipeModel.save()
                .then(doc => doc)
                .catch(reason => Promise
                        .reject(CustomValidation.messageValidation(reason)));
        },
        update(recipePayload) {
            return Recipe2
                .findById(recipePayload._id)
                .then(recipe => {
                    recipe.name = recipePayload.name;
                    recipe.updateDate = new Date();
                    recipe.categories = recipePayload.categories;
                    return recipe.save();
                }).catch(reason => Promise
                    .reject(CustomValidation.messageValidation(reason)));
        },
        get() {
            return Recipe2
                .find()
                .sort({ 'name': 1 })
                .then(recipes => {
                    return recipes.map(recipe => {
                        // mongoose sort is not working 
                        recipe.categories = recipe.categories
                            .sort((catA, catB) => catA.name > catB.name ? 1 : -1)
                        recipe.categories = UtilService
                            .sortAllProductCategory(recipe.categories)
                        return recipe    
                    })
                })
                .catch(reason => Promise
                    .reject(CustomValidation.messageValidation(reason)));
        },
        getOne(id) {
            return Recipe2.findById(id)
                .populate('categories')
                .then(recipe => recipe)
                .catch(reason => Promise
                    .reject(CustomValidation.messageValidation(reason)));
        }
    }
}

module.exports = RecipeService();
