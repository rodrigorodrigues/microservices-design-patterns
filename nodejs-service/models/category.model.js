const mongoose = require('mongoose');
mongoose.plugin(schema => { schema.options.usePushEach = true });
const product = require('./product.shema');

// TODO test if can reuse the cat scheme
const categorySchema = new mongoose.Schema({
    name: {
        type: String,
        minlength: 1,
        trim: true,
        unique: true,
        required: true
    },
    updateDate: Date,
    insertDate: Date,
    products: [product]
});

const Category = mongoose.model('Category', categorySchema);
module.exports = { Category };