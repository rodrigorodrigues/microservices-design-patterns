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
        async get(page = 0, size = 10) {
            try {
                const skip = page * size;

                // Get total count
                const totalElements = await Recipe2.countDocuments();

                // Get paginated data
                const recipes = await Recipe2
                    .find()
                    .sort({ 'name': 1 })
                    .skip(skip)
                    .limit(size);

                // Process recipes
                const processedRecipes = recipes.map(recipe => {
                    // mongoose sort is not working
                    recipe.categories = recipe.categories
                        .sort((catA, catB) => catA.name > catB.name ? 1 : -1)
                    recipe.categories = UtilService
                        .sortAllProductCategory(recipe.categories)
                    return recipe
                });

                // Calculate total pages
                const totalPages = Math.ceil(totalElements / size);

                // Build page response
                return {
                    content: processedRecipes,
                    number: page,
                    size: size,
                    totalPages: totalPages,
                    totalElements: totalElements,
                    first: page === 0,
                    last: page >= totalPages - 1
                };
            } catch (reason) {
                return Promise.reject(CustomValidation.messageValidation(reason));
            }
        },
        getOne(id) {
            return Recipe2.findById(id)
                .populate('categories')
                .then(recipe => recipe)
                .catch(reason => Promise
                    .reject(CustomValidation.messageValidation(reason)));
        },
        deleteById(id) {
            return Recipe2
            .findByIdAndRemove(id)
            .then(recipe => recipe)
            .catch(reason => Promise.reject(reason));
        }
    }
}

module.exports = RecipeService();
