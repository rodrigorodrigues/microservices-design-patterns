/**
 * Created by eliasmj on 27/11/2016.
 */

(function () {
    'use strict'

    var mongoose = require('mongoose');
    const mongoDbUri = process.env.MONGODB_URI;

    console.log(`Connecting to Mongodb: ${mongoDbUri}`);

    mongoose.Promise = global.Promise;
    mongoose.connect(mongoDbUri);

    module.exports = {
        mongoose : mongoose,
        connection : mongoose.connection
    }

})();