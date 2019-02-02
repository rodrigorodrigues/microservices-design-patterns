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
        products: [product]
    });


module.exports =  category;    