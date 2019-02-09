/**
 * Created by eliasmj on 28/11/2016.
 */

(function () {
    'use strict'

    var env = process.env.NODE_ENV || 'development';

    console.log("ENV ***", env)

    if(env === 'development') {
        let port = process.env.SERVER_PORT || 3002;
        process.env.PORT = port
        let mongodbUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/week_menu';
        process.env.MONGODB_CONNECTION = mongodbUri;
    } else if(env === 'test') {
        let port = process.env.SERVER_PORT || 3001;
        process.env.PORT = port
        let mongodbUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/week_menu_test';
        process.env.MONGODB_CONNECTION = mongodbUri;
    }

})();