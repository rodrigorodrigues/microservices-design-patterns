/**
 * Created by eliasmj on 24/11/2016.
 *
 * *** npm run dev >>> runs nodemon to reload the file without restart the server
 */
require('./config/config');
require('dotenv').load();

const log = require('./utils/log.message');

let app = require('express')();

const db = require('./db/mongoose');

const bodyParser = require('body-parser');

const port = process.env.PORT;

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

// Spring Boot Actuator
var actuator = require('express-actuator');
app.use(actuator('/actuator'));

// Prometheus
const promBundle = require("express-prom-bundle");
const metricsMiddleware = promBundle({includeMethod: true, metricsPath: '/actuator/prometheus'});
app.use(metricsMiddleware);

const eurekaServer = process.env.EUREKA_SERVER || '127.0.0.1';
const eurekaServerPort = process.env.EUREKA_PORT || 8761;
const ipAddr = process.env.IP_ADDRESS || '127.0.0.1';
console.log("eurekaServer: ", eurekaServer);
console.log("eurekaServerPort: ", eurekaServerPort);
console.log("port: ", port);
console.log("ipAddr: ", ipAddr);
// Eureka configuration
const eurekaClient = new Eureka({
    // application instance information
    instance: {
        app: 'WEEK-MENU-API',
        instanceId: 'WEEKMENUAPI',
        hostName: 'localhost',
        ipAddr: ipAddr,
        statusPageUrl: `http://${ipAddr}:${port}/actuator/info`,
        port: {
            '$': port,
            '@enabled': 'true',
        },
        vipAddress: 'WEEKMENUAPI',
        dataCenterInfo: {
            '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
            name: 'MyOwn',
        },
        registerWithEureka: true,
        fetchRegistry: true
    },
    eureka: {
        // eureka server host / port
        host: eurekaServer,
        port: eurekaServerPort,
        servicePath: '/eureka/apps/',
        maxRetries: 10
    }
});
eurekaClient.start();

// Spring Cloud Config
const springCloudConfig = require('spring-cloud-config');
const springProfilesActive = [];
const profile = (process.env.SPRING_PROFILES_ACTIVE || 'dev');
springProfilesActive.push(profile);
springProfilesActive.push("?X-Encrypt-Key=b7fc7cec8e7aab24648723258da87a8d09ad7cef7b0a2842738884496a9fbb53");
console.log("springProfilesActive: ", springProfilesActive);
let configOptions = {
    configPath: __dirname + '/config',
    activeProfiles: springProfilesActive,
    level: 'debug'
};

app.use(bodyParser.json());
// Create application/x-www-form-urlencoded parser
app.use(bodyParser.urlencoded({ extended: true }));

app.use(cors());
app.options('*', cors());

app.use(function (req, res, next) {

    let configProps = springCloudConfig.load(configOptions);
    configProps.then((config) => {
        const secretKey = config.configuration.jwt['base64-secret'];

        try {
            console.log("Path: ", req.path);
            if (req.path.startsWith('/actuator') || req.path === '/favicon.ico') {
                if (req.path === '/actuator') {
                    const jsonMessage = `{
                        "_links": {
                            "self": {
                                "href": "http://${ipAddr}:${port}/actuator",
                                "templated": false
                            },
                            "health": {
                                "href": "http://${ipAddr}:${port}/actuator/health",
                                "templated": false
                            },
                            "info": {
                                "href": "http://${ipAddr}:${port}/actuator/info",
                                "templated": false
                            },
                            "prometheus": {
                                "href": "http://${ipAddr}:${port}/actuator/prometheus",
                                "templated": false
                            },
                            "metrics": {
                                "href": "http://${ipAddr}:${port}/actuator/metrics",
                                "templated": false
                            }
                        }
                    }`;
                    res.status(200).send(JSON.parse(jsonMessage));
                } else if (req.path == '/actuator/health') {
                    res.status(200).send(JSON.parse('{"status": "UP"}'));
                } else {
                    res.sendStatus(200);
                }
            } else {
                console.log("Headers", req.headers);
                let token = req.headers.authorization;
                if (!token) {
                    throw err("Token Not found");
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
            console.log("Error validate JWT", e);
            res.status(401).send("Error on validate JWT: "+e);
        }
    });

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