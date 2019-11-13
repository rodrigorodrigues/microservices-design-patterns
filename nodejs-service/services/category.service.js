const { Category } = require('../models/category.model');
const CustomValidation = require('../services/custom.validation');
const RecipeSubdocService = require('./recipe.subdoc.service');
const UtilService = require('./util')

function CategoryService() {
    return {
        save(category) {
            const categoryModel = new Category({
                name: category.name,
                insertDate: new Date(),
            });
            return categoryModel.save()
                .then(doc => doc)
                .catch(reason => {
                    return Promise
                        .reject(CustomValidation.messageValidation(reason));
                });
        },
        update(category) {
            return Category
                .findOne({ _id: category._id })
                .then(updateCategory.bind(null, category))
                .catch(reason => {
                    return Promise
                        .reject(CustomValidation.messageValidation(reason));
                });
        },
        addProduct(product, id) {
            return Category.findOne({ _id: id })
                .then(category => {
                    category.products.push(product);
                    return category.save();
                }).catch(reason => {
                    return Promise.reject(reason);
                });
        },
        get() {
            return Category
                .find()
                .sort({ 'name': 1 })
                .then(UtilService.sortAllProductCategory)
                .catch(reason => Promise.reject(reason));
        },
        getById(id) {
            return Category
            .findOne({ _id: id })
            .sort({ 'name': 1 })
            .then(category => category)
            .catch(reason => Promise.reject(reason));
        },
        deleteById(id) {
            return Category
            .findByIdAndRemove(id)
            .then(category => category)
            .catch(reason => Promise.reject(reason));
        }
    }
}

//TODO temporary fn for avoid duplication
function isProductExists() {
    Category
        .find({})
}

function updateCategory(category, doc) {
    doc.name = category.name;
    // Update all subs
    return doc.save()
        .then(doc => RecipeSubdocService.updateCategory(doc._id, doc.name))
        .catch(reason => Promise.reject(reason))
}

module.exports = CategoryService();

