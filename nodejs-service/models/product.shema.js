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
        }
    });

module.exports =  product;    