const { Category } = require('../models/category.model');
const CustomValidation = require('../services/custom.validation');
const RecipeSubdocService = require('./recipe.subdoc.service');
const UtilService = require('./util')

function CategoryService() {
    return {
        save(category, user) {
            const categoryModel = new Category({
                name: category.name,
                insertDate: new Date(),
                _user: user.sub
            });
            return categoryModel.save()
                .then(doc => doc)
                .catch(reason => {
                    return Promise
                        .reject(CustomValidation.messageValidation(reason));
                });
        },
        update(category, user) {
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
        async get(page = 0, size = 10) {
            console.log("Finding categories...");
            try {
                const skip = page * size;

                // Get total count
                const totalElements = await Category.countDocuments();

                // Get paginated data
                const categories = await Category
                    .find()
                    .sort({ 'name': 1 })
                    .skip(skip)
                    .limit(size);

                // Sort products within categories
                const sortedCategories = UtilService.sortAllProductCategory(categories);

                // Calculate total pages
                const totalPages = Math.ceil(totalElements / size);

                // Build page response
                return {
                    content: sortedCategories,
                    number: page,
                    size: size,
                    totalPages: totalPages,
                    totalElements: totalElements,
                    first: page === 0,
                    last: page >= totalPages - 1
                };
            } catch (reason) {
                return Promise.reject(reason);
            }
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

