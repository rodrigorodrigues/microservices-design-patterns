const mongoose = require('mongoose');
const category = require('./category.shema');

const recipeSchema = new mongoose.Schema({
    name: {
        type: String,
        minlength: 1,
        trim: true,
        required: true,
        unique: true
    },
    updateDate: {
        type: Date
    },
    insertDate: {
        type: Date
    },
    categories: {
        type: [category],
        required: true
    },
    _user: {
        type: String,
        default: 'default@admin.com'
    }
});
recipeSchema.plugin(require('mongoose-audit'), {connection: mongoose.connection, logCollection: 'audit_recipe2'});
const Recipe2 = mongoose.model('Recipe2', recipeSchema);

module.exports = { Recipe2 };