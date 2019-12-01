/**
 * Created by eliasmj on 29/11/2016.
 */

(function () {
    'use strict'

    const _ = require('lodash');

    const logOnRoutes = function () {
        //if (process.env.NODE_ENV !== 'test') {
            if (arguments.length === 2) {
                console.log("Log " + getLogDate(), arguments[0], filterObject(arguments[1]));
            } else {
                console.log("Log " + getLogDate(), filterObject(arguments[0]));
            }
        //}
    }

    var error = function () {
            if (arguments.length === 2) {
                console.error("Error " + getLogDate(), arguments[0], arguments[1]);
            } else {
                console.error("Error " + getLogDate(), arguments[0]);
            }
    }

    function filterObject(object) {
        if (_.isArray(object)) {
            let result = "Total Items " + object.length + ",  \n";
            let names = '';
            object.forEach(doc => {
                names += "  * " + _.get(doc, 'name', '') + " \n"
            });
            result += names;
            return result;
        } else {
            return object;
        }
    }
    function getLogDate() {
        let date = new Date();
        return date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds();
    }
    module.exports = { logOnRoutes, error };
})();