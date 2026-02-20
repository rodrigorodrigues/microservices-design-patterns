/**
 * Created by eliasmj on 15/08/2016.
 */
(function () {
    'use strict'

    const mongoose = require('mongoose');
    const Schema = mongoose.Schema;

    const recipeSchema = new mongoose.Schema({
        name: {
            type: String,
            minlength: 1,
            trim: true,
            required: true,
            unique: true
        },
        updateDate: Date,
        insertDate: Date,
        categories: [{ type: Schema.Types.ObjectId, ref: 'Category' }],
        weekDay: String,
        isInMenuWeek: {
            type: Boolean,
            default: false
        },
        mainMealValue: String,
        description: String,
        attributes: [{ ref: 'IngredientRecipeAttributes', type: Schema.Types.ObjectId }],
        _user: {
            type: String,
            default: 'default@admin.com'
        }
    });
    recipeSchema.plugin(require('mongoose-audit'), {connection: mongoose.connection, logCollection: 'audit_recipes'});
    const Recipe = mongoose.model('Recipe', recipeSchema);

    module.exports = { Recipe };

})();
