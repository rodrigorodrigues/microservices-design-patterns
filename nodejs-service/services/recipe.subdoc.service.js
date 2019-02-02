const { Recipe2 } = require('../models/recipe2.model');

const RecipeSubdocService = {
    updateCategory(_id, newName) {
        Recipe2
            .find({})
            .then(updateCategory.bind(null, _id, newName))
            .catch(reason => {
                console.error('Error update docs, how to notify ?', reason)
            });
    },
    updateProduct(_id, newName) {
        Recipe2
            .find({})
            .then(updateProduct.bind(null, _id, newName))
            .catch(reason => {
                console.error('Error update docs, how to notify ?', reason)
            });
    }
}

function updateCategory(_id, newName, recipes) {
    recipes.forEach(recipe => {
        const cat = recipe.categories.id(_id);
        if (cat) {
            cat.name = newName;
            recipe.save();
        }
    });
}

function updateProduct(_id, newName, recipes) {
    //TODO Mongo should have a query for this
    recipes.forEach(recipe => {
        recipe.categories.forEach(category => {
            const prod = category.products.id(_id);
            if (prod) {
                prod.name = newName;
                recipe.save();
            }
        });
    });
}

module.exports = RecipeSubdocService;