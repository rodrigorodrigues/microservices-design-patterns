const mongoose = require('mongoose');
mongoose.plugin(schema => { schema.options.usePushEach = true });
const Schema = mongoose.Schema;

const product = new Schema(
    {
        name: {
            type: 'string',
            trim: true,
            required: true
        },
        insertDate: {
            type: Date
        },
        completed: {
            type: Boolean,
            default: false
        },
        quantity: {
            type: Number,
            default: 1
        },
        _user: {
            type: String,
            default: 'default@admin.com'
        }
    });
product.plugin(require('mongoose-audit'), {connection: mongoose.connection, logCollection: 'audit_products'});
module.exports =  product;    