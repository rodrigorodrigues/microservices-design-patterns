const mongoose = require('mongoose');
mongoose.plugin(schema => { schema.options.usePushEach = true });
const Schema = mongoose.Schema;
const product = require('./product.shema');

const category = new Schema(
    {
        name: {
            type: 'string',
            unique: false
        },
        insertDate: {
            type: Date
        },
        products: [product],
        _user: {
            type: String,
            default: 'default@admin.com'
        }
    });

category.plugin(require('mongoose-audit'), {connection: mongoose.connection, logCollection: 'audit_category_schema'});
module.exports =  category;    