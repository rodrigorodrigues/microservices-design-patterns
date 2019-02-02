const mongoose = require('mongoose');
const Schema = mongoose.Schema;
const category = require('./category.shema');

const recipeSchema = new Schema({
    name: {
        type: String,
        minlength: 1,
        trim: true,
        required: true
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
    }
});

module.exports = recipeSchema;