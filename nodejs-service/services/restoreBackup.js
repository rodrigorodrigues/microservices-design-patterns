const { Recipe } = require('../models/recipe.model');
const categoryFile = require('../db_backup/categories.json');
const ingredFile = require('../db_backup/ingredients.json');
const recipeFile = require('../db_backup/recipes.json');
const attrsFile = require('../db_backup/attributes.json');
const { Recipe2 } = require('../models/recipe2.model');

function restoreBackup() {
    const recipes = JSON.parse(JSON.stringify(recipeFile));
    const cats = JSON.parse(JSON.stringify(categoryFile));
    const ingreds = JSON.parse(JSON.stringify(ingredFile));
    const attrs = JSON.parse(JSON.stringify(attrsFile));
    Recipe.remove({}).then(() => {
        // RECIPES
        recipes.forEach(rec => {
            let cats = [];
            console.log('-----------------------------', rec.name);
            rec.attributes.forEach(id => {
                const attr = getAttr(id.$oid);
                const prod = getIngredient(attr.ingredientId.$oid);
                const cat = getCategoty(prod._creator.$oid);
                if (cats[cat.name]) {
                    const obj = {
                        name: prod.name,
                        insertDate: new Date()
                    };
                    cats[cat.name].products.push(obj);
                }
                else {
                    const obj = {
                        name: prod.name,
                        insertDate: new Date()
                    };
                    cats[cat.name] = {
                        name: cat.name,
                        insertDate: new Date(),
                        products: [obj]
                    };
                }
            });
            const categories = Object.keys(cats).map(k => cats[k]);
            const recipeModel = new Recipe2({
                name: rec.name,
                insertDate: new Date(),
                categories: categories
            });
            recipeModel.save();
            //    categories.forEach( c => {
            //     console.log(c.name)
            //     console.log(c.products)
            //    })
        });
    });
    function getAttr(id) {
        return attrs.find(attr => {
            //console.log('attr ************ '+(attr._id.$oid === id), attr._id.$oid, id)
            return attr._id.$oid === id;
        });
    }
    function getIngredient(id) {
        return ingreds.find(ing => {
            return ing._id.$oid == id;
        });
    }
    function getCategoty(id) {
        return cats.find(cat => cat._id.$oid === id);
    }
}

module.exports = restoreBackup;
