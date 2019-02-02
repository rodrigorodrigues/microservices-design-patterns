/**
 * Created by eliasmj on 13/12/2016.
 */

(function () {
    'use strict'

    const mongoose = require('mongoose');
    const Schema = mongoose.Schema;

    const attributesSchema = new mongoose.Schema({
            labelQuantity: {
                type: String
            },
            name: String,
            quantity: Number,
            itemSelectedForShopping: {
                type: Boolean,
                default: true
            },
            ingredientId : {type : Schema.Types.ObjectId, ref: 'Ingredient', required: true},
            recipeId : {type : Schema.Types.ObjectId, ref: 'Recipe', required: true},
            checkedInCartShopping: {
                type: Boolean,
                default: true
            },
            isRecipeLinkedToCategory : {
                type: Boolean,
                default: true
            }

        });

    const IngredientRecipeAttributes = mongoose.model('IngredientRecipeAttributes', attributesSchema);

    module.exports = {IngredientRecipeAttributes};
})();