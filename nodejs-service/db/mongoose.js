/**
 * Created by eliasmj on 27/11/2016.
 */

(function () {
    'use strict'

    var mongoose = require('mongoose');

    mongoose.Promise = global.Promise;
    mongoose.connect(process.env.MONGODB_URI);

    module.exports = {
        mongoose : mongoose,
        connection : mongoose.connection
    }

})();