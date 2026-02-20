const mongoose = require('mongoose');
const category = require('./category.shema');
const recipe = require('./recipe.scheme');

const shoppingListSchema = new mongoose.Schema({
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
    },
    _user: {
        type: String,
        default: 'default@admin.com'
    }
});
shoppingListSchema.plugin(require('mongoose-audit'), {connection: mongoose.connection, logCollection: 'audit_shopping_lists'});
const ShoppingList = mongoose.model('ShoppingList', shoppingListSchema);
module.exports = {ShoppingList};