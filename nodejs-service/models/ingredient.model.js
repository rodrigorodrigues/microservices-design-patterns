/**
 * Created by eliasmj on 27/11/2016.
 */

(function () {
    'use strict'

    const mongoose = require('mongoose');
    const Schema = mongoose.Schema;

    const ingredientSchema = new mongoose.Schema({
        name: {
            type: String,
            minlength: 1,
            trim: true,
            required: true,
            unique: true
        },
        updateDate: Date,
        insertDate: Date,
        _creator : {
            type : Schema.Types.ObjectId,
            ref: 'Category'
        },
        categoryName: {
            type: String,
            minlength: 1,
            trim: true
        },
        expiryDate: {
            type: Date,
            default: Date.now
        },
        //Last checking date from the shopping list after been checked
        updateCheckDate: {
            type: Date,
            default: Date.now
        },
        //for the recipe list, ingredient that needs to buy, some ingredient is in the recipe but does not
        //need to buy
        checkedInCartShopping: Boolean,
        tempRecipeLinkIndicator: {
            type: Boolean,
            default: true
        },
        attributes: [{ref: 'IngredientRecipeAttributes', type: Schema.Types.ObjectId }],
        _user: {
            type: String,
            default: 'default@admin.com'
        }
    });

    let transientAttribute;

    //does not serve para nada
    ingredientSchema.virtual('currentRecipeAttribute')
        .get(() => {
            return transientAttribute;
        }).set(value => {
            transientAttribute = value;
        });

    ingredientSchema.plugin(require('mongoose-audit'), {connection: mongoose.connection});
    const Ingredient = mongoose.model('Ingredient',ingredientSchema);

    module.exports = {Ingredient};

})();