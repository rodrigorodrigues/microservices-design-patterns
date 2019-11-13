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

var initializeSwagger = require('swagger-tools').initializeMiddleware;

const port = process.env.PORT;

const logger = function (request, response, next) {
    console.log("Request body: ", request.body);
    console.log("Request METHOD: ", request.method);
    console.log("Request resource: ", request.path);

    next();
}

const recipeRouter = require('./routes/recipe.route');
const ingredientRouter = require('./routes/ingredient.route');
const categoryRouter = require('./routes/category.route');
const restoreBackup = require("./services/restoreBackup");
const productRouter = require('./routes/product.route');
const category2Router = require('./routes/category2.route');
const recipe2Router = require('./routes/recipe2.route');
const shoppingListRouter = require('./routes/shopping.list.route');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const Eureka = require('eureka-js-client').Eureka;

//Swagger
loadSwagger();

// Spring Boot Actuator
loadActuator();

// Prometheus
loadPrometheus();

const { hostName, ipAddr, eurekaServer, eurekaServerPort, zipkinHost, zipkinPort, restoreMongoDb, eurekaServerPath } = loadEnvVariables();

// Eureka configuration
loadEureka();

// Spring Cloud Config
var { springCloudConfig, configOptions } = loadSpringCloudConfig();

//Zipkin
loadZipkin();

let secretKey = null;

app.use(bodyParser.json());
// Create application/x-www-form-urlencoded parser
app.use(bodyParser.urlencoded({ extended: true }));

app.use(cors());
app.options('*', cors());


app.use(function (req, res, next) {

    if (req.path.startsWith('/actuator') || req.path === '/favicon.ico') {
        actuatorRoute(req, res);
    } else if (req.path.startsWith('/docs') || req.path.startsWith('/api-docs')) {
        next();
    } else {
        validateJwt(req, res, next);
    }

});

//Spring Cloud Sleuth
loadSleuth();

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
    console.log("MongoDB successful connected");
    if (restoreMongoDb) {
        console.log("Applying Restore MongoDB for connection: ", process.env.MONGODB_CONNECTION);
        db.connection.db.dropCollection('categories');
        db.connection.db.dropCollection('ingredients');
        db.connection.db.dropCollection('recipe2');
        db.connection.db.dropCollection('recipes');
        db.connection.db.dropCollection('shoppinglists');
        restoreBackup();
    }
});

app.listen(port, () => {
    console.log("Application started. Listening on port:" + port);
    loadSecretKey();
    generateSwaggerJsonFile();
});

function loadSleuth() {
    const morgan = require('morgan');
    app.use(morgan(function (tokens, req, res) {
        return [
            tokens.method(req, res),
            '[TRACE-ID:' + req.header('X-B3-Traceid') + ']',
            tokens.url(req, res),
            tokens.status(req, res),
            tokens.res(req, res, 'content-length'), '-',
            tokens['response-time'](req, res), 'ms'
        ].join(' ');
    }));
}

function loadActuator() {
    var actuator = require('express-actuator');
    app.use(actuator('/actuator'));
}

function loadSwagger() {
    const swaggerJSDoc = require('swagger-jsdoc');

    const options = {
        definition: {
          swagger: '2.0', // Specification (optional, defaults to swagger: '2.0')
          info: {
            title: 'Hello World', // Title (required)
            version: '1.0.0', // Version (required)
          },
        },
        // Path to the API docs
        apis: ['./routes/*.route.js'],
      };
      
      // Initialize swagger-jsdoc -> returns validated swagger spec in json format
      const swaggerSpec = swaggerJSDoc(options);

      app.get('/api-docs.json', (req, res) => {
        res.setHeader('Content-Type', 'application/json');
        res.send(swaggerSpec);
      });
}

function generateSwaggerJsonFile() {
    var request = require('request');
    request.get(`http://localhost:${port}/api-docs.json`, function(error, response, body) {
        var fs = require('fs');

        console.log("body", body);

        fs.writeFileSync("/tmp/swagger.json", body)

        loadSwaggerUI();
    });
}

function loadSwaggerUI() {
    const swaggerDoc = require('/tmp/swagger.json');
    console.log("swaggerDoc", swaggerDoc);

    // Initialize the Swagger middleware
    initializeSwagger(swaggerDoc, function (middleware) {
        console.log("initializeMiddleware:init");

        // Interpret Swagger resources and attach metadata to request - must be first in swagger-tools middleware chain
        app.use(middleware.swaggerMetadata());

        // Provide the security handlers
        app.use(middleware.swaggerSecurity({
            oauth2: function (req, def, scopes, callback) {
            // Do real stuff here
            }
        }));

        // Validate Swagger requests
        app.use(middleware.swaggerValidator({
            validateResponse: true
        }));

        // Route validated requests to appropriate controller
        //app.use(middleware.swaggerRouter({useStubs: true, controllers: './controllers'}));

        // Serve the Swagger documents and Swagger UI
        //   http://localhost:3000/docs => Swagger UI
        //   http://localhost:3000/api-docs => Swagger document
        app.use(middleware.swaggerUi({
            //apiDocs: `http://localhost:${port}/api-docs`,
        }));


        console.log("initializeMiddleware:nd");
    });
}

function loadPrometheus() {
    const promBundle = require("express-prom-bundle");
    const metricsMiddleware = promBundle({ includeMethod: true, metricsPath: '/actuator/prometheus' });
    app.use(metricsMiddleware);
}

function loadEnvVariables() {
    const eurekaServer = process.env.EUREKA_SERVER || '127.0.0.1';
    const eurekaServerPort = process.env.EUREKA_PORT || 8761;
    const eurekaServerPath = process.env.EUREKA_PATH || '/eureka/apps/';
    const ipAddr = process.env.IP_ADDRESS || '127.0.0.1';
    const hostName = process.env.HOST_NAME || 'localhost';
    const zipkinHost = process.env.ZIPKIN_HOST || 'localhost';
    const zipkinPort = process.env.ZIPKIN_PORT || 9411;
    const restoreMongoDb = process.env.RESTORE_MONGODB || true;
    console.log("eurekaServer: ", eurekaServer);
    console.log("eurekaServerPort: ", eurekaServerPort);
    console.log("eurekaServerPath: ", eurekaServerPath);
    console.log("port: ", port);
    console.log("ipAddr: ", ipAddr);
    console.log("hostName: ", hostName);
    console.log("zipkinHost: ", zipkinHost);
    console.log("zipkinPort: ", zipkinPort);
    console.log("restoreMongoDb: ", restoreMongoDb);
    return { hostName, ipAddr, eurekaServer, eurekaServerPort, zipkinHost, zipkinPort, restoreMongoDb, eurekaServerPath };
}

function loadSpringCloudConfig() {
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
    return { springCloudConfig, configOptions };
}

function loadEureka() {
    const eurekaClient = new Eureka({
        // application instance information
        instance: {
            app: 'WEEK-MENU-API',
            instanceId: 'WEEK-MENU-API',
            hostName: hostName,
            ipAddr: ipAddr,
            statusPageUrl: `http://${ipAddr}:${port}/actuator/info`,
            healthCheckUrl: `http://${ipAddr}:${port}/actuator/health`,
            homePagekUrl: `http://${ipAddr}:${port}`,
            port: {
                '$': port,
                '@enabled': 'true',
            },
            vipAddress: 'WEEK-MENU-API',
            dataCenterInfo: {
                '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
                name: 'MyOwn',
            }
        },
        eureka: {
            // eureka server host / port
            host: eurekaServer,
            port: eurekaServerPort,
            servicePath: eurekaServerPath,
            maxRetries: 10,
            registerWithEureka: true,
            fetchRegistry: true
        }
    });
    eurekaClient.start();
}

function loadZipkin() {
    const zipkinEnabled = process.env.ZIPKIN_ENABLED || false;
    if (zipkinEnabled) {
        /* eslint-env browser */
        const { BatchRecorder, jsonEncoder: { JSON_V2 } } = require('zipkin');
        const { HttpLogger } = require('zipkin-transport-http');
        // Send spans to Zipkin asynchronously over HTTP
        const zipkinBaseUrl = `http://${zipkinHost}:${zipkinPort}`;
        const recorder = new BatchRecorder({
            logger: new HttpLogger({
                endpoint: `${zipkinBaseUrl}/api/v2/spans`,
                jsonEncoder: JSON_V2
            })
        });
        const CLSContext = require('zipkin-context-cls');
        const { Tracer } = require('zipkin');
        const ctxImpl = new CLSContext('zipkin');
        const localServiceName = 'weel-menu-api';
        const tracer = new Tracer({ ctxImpl, recorder, localServiceName });
        // instrument the server
        const zipkinMiddleware = require('zipkin-instrumentation-express').expressMiddleware;
        app.use(zipkinMiddleware({ tracer }));
    }
}

function loadSecretKey() {
    let configProps = springCloudConfig.load(configOptions);
    configProps.then((config) => {
        secretKey = config.configuration.jwt['base64-secret'];
        if (secretKey === "" || secretKey === null || secretKey === undefined) {
            const pathPublicKey = process.env.PATH_PUBLIC_KEY;
            console.log("pathPublicKey: ", pathPublicKey);
            var fs = require('fs');
            secretKey = fs.readFileSync(pathPublicKey);
        } 
        console.log("SecretKey: ", secretKey);
    });
}

function validateJwt(req, res, next) {
    try {
        console.log("Headers", req.headers);
        let token = req.headers.authorization;
        if (!token) {
            throw Error("Token Not found");
        }
        token = token.replace("Bearer ", "");
        jwt.verify(token, new Buffer(secretKey, 'base64'), function(err, decoded) {
            req.user = decoded;
        });

        if ('OPTIONS' == req.method) {
            res.sendStatus(200);
        }
        else {
            next();
        }
    } catch (e) {
        console.log("Error validate JWT", e);
        res.status(401).send("Error on validate JWT: " + e);
    }
}

function actuatorRoute(req, res) {
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
    }
    else if (req.path == '/actuator/health') {
        res.status(200).send(JSON.parse('{"status": "UP"}'));
    }
    else {
        res.sendStatus(200);
    }
}

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