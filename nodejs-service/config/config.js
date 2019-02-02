/**
 * Created by eliasmj on 28/11/2016.
 */

(function () {
    'use strict'

    var env = process.env.NODE_ENV || 'development';

    console.log("ENV ***", env)

    if(env === 'development') {
        process.env.PORT = 3002;
        process.env.MONGODB_URI = 'mongodb://localhost:27017/week_menu';
    } else if(env === 'test') {
        process.env.PORT = 3001;
        process.env.MONGODB_URI = 'mongodb://localhost:27017/week_menu_test';
    }
    process.env.SECRET_KEY = 'YTMwOTIwODE1MGMzOGExM2E4NDc5ZjhjMmQwMTdkNDJlZWZkOTE0YTMwNWUxMTgxMTFhZTI1ZDI3M2QyMTRmMjI5Yzg0ODBjYTUxYjVkY2I5ZmY0YmRkMzBlZjRjNDM2Y2NiYzhlZjQ0ODRjMWZlNzVmZjdjM2JiMjdkMjdmMjk=';

})();