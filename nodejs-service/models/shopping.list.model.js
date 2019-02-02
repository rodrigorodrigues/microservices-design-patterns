const moongoose = require('mongoose');
const category = require('./category.shema');
const recipe = require('./recipe.scheme');

const shoppingListSchema = new moongoose.Schema({
    categories: [category],
    recipes: [recipe],
    updateDate: {
        type: Date
    },
    insertDate: {
        type: Date
    },
    name: {
        type: String
    }
});
const ShoppingList = moongoose.model('ShoppingList', shoppingListSchema);
module.exports = {ShoppingList};