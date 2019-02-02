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
});

const Recipe2 = mongoose.model('Recipe2', recipeSchema);

module.exports = { Recipe2 };