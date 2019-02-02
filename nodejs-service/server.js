/**
 * Created by eliasmj on 24/11/2016.
 *
 * *** npm run dev >>> runs nodemon to reload the file without restart the server
 */
require('./config/config');

const log = require('./utils/log.message');

let app = require('express')();

const db = require('./db/mongoose');

const bodyParser = require('body-parser');

const port = process.env.PORT;

const secretKey = process.env.SECRET_KEY;

//TODO security change this later
const whiteList = ['localhost:8100', 'localhost:3000', 'localhost:3002', '109.255.172.3:8090'];

const logger = function (request, response, next) {
    log.logExceptOnTest("Request body: ", request.body);
    log.logExceptOnTest("Request METHOD: ", request.method);
    log.logExceptOnTest("Request resource: ", request.path);

    next();
}

const recipeRouter = require('./routes/recipe.route');
const ingredientRouter = require('./routes/ingredient.route');
const categoryRouter = require('./routes/category.route');
const productRouter = require('./routes/product.route');
const category2Router = require('./routes/category2.route');
const recipe2Router = require('./routes/recipe2.route');
const shoppingListRouter = require('./routes/shopping.list.route');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const Eureka = require('eureka-js-client').Eureka;

// example configuration
const client = new Eureka({
    // application instance information
    instance: {
      app: 'week-menu-api',
      hostName: '127.0.0.1',
      ipAddr: '127.0.0.1',
      statusPageUrl: 'http://127.0.0.1:3002/actuator/info',
      port: {
        '$': 3002,
        '@enabled': 'true',
      },
      vipAddress: '127.0.0.1',
      dataCenterInfo: {
        '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
        name: 'MyOwn',
      }
    },
    eureka: {
      // eureka server host / port
      host: '127.0.0.1',
      port: 8761,
      servicePath: '/eureka/apps/'
    },
});
client.start();

app.use(bodyParser.json());
// Create application/x-www-form-urlencoded parser
app.use(bodyParser.urlencoded({ extended: true }));

app.use(cors());
app.options('*', cors());

app.use(function (req, res, next) {

    try{
        console.log("Path: ", req.path);
        if (req.path.startsWith('/actuator/') || req.path === '/favicon.ico') {
            res.sendStatus(200);
        } else {
            console.log("Headers", req.headers);
            let token = req.headers.authorization;
            if (!token) {
                throw "Token Not found";
            }
            token = token.replace("Bearer ", "");
            console.log("Token: ", token);
            jwt.verify(token, new Buffer(secretKey, 'base64'));

            if ('OPTIONS' == req.method) {
                res.sendStatus(200);
            }
            else {
                next();
            }
        }
    } catch(e) {
        console.log("Error validation JWT", e);
        res.sendStatus(401);
    }

});

app.use(logger);

app.use('/', recipeRouter);
app.use('/', ingredientRouter);
app.use('/', categoryRouter);

app.use('/v2', productRouter);
app.use('/v2', category2Router);
app.use('/v2', recipe2Router);
app.use('/v2', shoppingListRouter);

app.use(errorHandle);

app.get('/', (req, res) => {
    res.send("Root api")
});

db.connection.on('error', () => {
    log.errorExceptOnTest('Oops Something went wrong, connection error:');
});

db.connection.once('open', () => {
    log.logExceptOnTest("MongoDB successful connected");
});

app.listen(port, () => {
    log.logExceptOnTest("Application started. Listening on port:" + port);
});

function errorHandle(err, req, res, next) {
    log.errorExceptOnTest('server.js', err.stack);

    const errorResponse = {
        message: err.message,
        name: "Main error",
        errors: []
    };

    res
        .status(500) //bad format
        .send(errorResponse)
        .end();
}

module.exports = { app: app };