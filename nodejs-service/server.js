/**
 * Created by eliasmj on 24/11/2016.
 *
 * *** npm run dev >>> runs nodemon to reload the file without restart the server
 */
require('dotenv').config();

const log = require('./utils/log.message');

let app = require('express')();

const db = require('./db/mongoose');

const bodyParser = require('body-parser');

const port = process.env.SERVER_PORT || 3002;

const recipeRouter = require('./routes/recipe.route');
const ingredientRouter = require('./routes/ingredient.route');
const categoryRouter = require('./routes/category.route');
const restoreBackup = require("./services/restoreBackup");
const productRouter = require('./routes/product.route');
const category2Router = require('./routes/category2.route');
const recipe2Router = require('./routes/recipe2.route');
const shoppingListRouter = require('./routes/shopping.list.route');
const jwt = require('jsonwebtoken');
const consulClient = require("consul");
const yamlClient = require('yaml');

const {
    hostName,
    consulServer,
    consulServerPort,
    zipkinHost,
    zipkinPort,
    restoreMongoDb,
    consulEnabled,
    swaggerEnabled,
    jwksValidation} = loadEnvVariables();

if (process.env.NODE_ENV !== 'test') {
    app.emit('ready');
}    

app.use(bodyParser.json());
// Create application/x-www-form-urlencoded parser
app.use(bodyParser.urlencoded({ extended: true }));

app.use(async function (req, res, next) {

    if (req.path.startsWith('/actuator') || req.path === '/favicon.ico') {
        actuatorRoute(req, res);
    } else if (req.path.startsWith('/docs') || req.path.startsWith('/api-docs')) {
        next();
    } else {
        await validateJwt(req, res, next);
    }

});

app.use('/', recipeRouter);
app.use('/', ingredientRouter);
app.use('/', categoryRouter);

app.use('/v2', productRouter);
app.use('/v2', category2Router);
app.use('/v2', recipe2Router);
app.use('/v2', shoppingListRouter);

let server;
app.on('ready', function() {
    server = app.listen(port, () => {
        console.log("Application started. Listening on port:" + port);
        if (consulEnabled === 'false') {
            secretKey = process.env.SECRET_TOKEN;
            console.log(`Using secretKey from env: ${secretKey}`);
        }
        if (swaggerEnabled === 'true') {
            generateSwaggerJsonFile();
        }
    });
});

app.on('close', function() {
    if (server) {
        server.close();
    }
});

// Eureka configuration
if (consulEnabled === 'true') {
    loadConsul();
}

// Health Check Actuator
loadActuator();

//Spring Cloud Sleuth
loadSleuth();

//Swagger
if (swaggerEnabled === 'true') {
    loadSwagger();
}

// Prometheus
loadPrometheus();

//Zipkin
loadZipkin();

let secretKey = null;

db.connection.on('error', (err) => {
    log.error(`Oops Something went wrong, connection error: ${err.stack}`);
});

db.connection.once('open', () => {
    console.log("MongoDB successful connected");
    if (restoreMongoDb === 'true') {
        console.log("Applying Restore MongoDB for connection: ", process.env.MONGODB_URI);
        if (process.env.NODE_ENV !== 'test') {
            try {
                db.connection.db.dropCollection('categories');
                db.connection.db.dropCollection('ingredients');
                db.connection.db.dropCollection('recipe2');
                db.connection.db.dropCollection('recipes');
                db.connection.db.dropCollection('shoppinglists');
            } catch (err) {
                console.error(err.stack);
            }
        }
        restoreBackup();
    }
    app.emit('ready');
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
            title: 'API - Week Menu Service', // Title (required)
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

        fs.writeFileSync("/tmp/swagger.json", body)

        loadSwaggerUI();
    });
}

function loadSwaggerUI() {
    var initializeSwagger = require('swagger-tools').initializeMiddleware;

    const swaggerDoc = require('/tmp/swagger.json');

    // Initialize the Swagger middleware
    initializeSwagger(swaggerDoc, function (middleware) {
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
    });
}

function loadPrometheus() {
    const promBundle = require("express-prom-bundle");
    const metricsMiddleware = promBundle({ includeMethod: true, metricsPath: '/actuator/prometheus' });
    app.use(metricsMiddleware);
}

function loadEnvVariables() {
    const consulServer = process.env.CONSUL_SERVER || '127.0.0.1';
    const consulServerPort = process.env.CONSUL_PORT || 8500;
    const hostName = process.env.HOST_NAME || 'localhost';
    const zipkinHost = process.env.ZIPKIN_HOST || 'localhost';
    const zipkinPort = process.env.ZIPKIN_PORT || 9411;
    const restoreMongoDb = process.env.RESTORE_MONGODB || 'false';
    const consulEnabled = process.env.CONSUL_ENABLED || 'true';
    const swaggerEnabled = process.env.SWAGGER_ENABLED || 'true';
    const jwksValidation = process.env.JWKS_VALIDATION || 'false';
    console.log("consulServer: ", consulServer);
    console.log("consulServerPort: ", consulServerPort);
    console.log("port: ", port);
    console.log("hostName: ", hostName);
    console.log("zipkinHost: ", zipkinHost);
    console.log("zipkinPort: ", zipkinPort);
    console.log("restoreMongoDb: ", restoreMongoDb);
    console.log("consulEnabled: ", consulEnabled);
    console.log("swaggerEnabled: ", swaggerEnabled);
    console.log("jwksValidation: ", jwksValidation);
    return { hostName, consulServer, consulServerPort, zipkinHost, zipkinPort, restoreMongoDb, consulEnabled, swaggerEnabled, jwksValidation };
}

async function loadConsul() {
    const consul = new consulClient({
        host: consulServer,
        port: consulServerPort
    });
    consul.agent.service.register({
        name: 'WEEK-MENU-API',
        address: hostName,
        port: port,
        check: {
            http: `http://${hostName}:${port}/actuator/health`,
            interval: '30s'
        }
    }, () => {
        console.log(`Service WEEK-MENU-API registered`);
    });
    await loadSecretKey(consul);
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

async function loadSecretKey(consul) {
    const configKey = process.env.CONSUL_CONFIG_KEY || 'config/week-menu-api/data';

    try {
        const result = await consul.kv.get(configKey);

        if (result && result.Value) {
            const config = yamlClient.parse(result.Value);
            secretKey = config['com']['microservice']['authentication']['jwt']['keyValue'];

            if (secretKey) {
                console.log('secretKey loaded from Consul');
            } else {
                console.log('secretKey not found in Consul config, using fallback');
                fallbackSecretKey();
            }
        } else {
            console.log('No config found in Consul, using fallback');
            fallbackSecretKey();
        }
    } catch (err) {
        console.error('Error fetching config from Consul:', err);
        fallbackSecretKey();
    }
}

function fallbackSecretKey() {
    const pathPublicKey = process.env.PATH_PUBLIC_KEY;
    if (pathPublicKey) {
        const fs = require('fs');
        try {
            secretKey = fs.readFileSync(pathPublicKey, 'utf8');
            console.log('secretKey loaded from file');
        } catch (err) {
            console.error('Error reading secret key from file:', err);
        }
    }
}

async function validateJwt(req, res, next) {
    try {
        let token = req.headers.authorization;
        if (!token) {
            res.status(400).send("Token Not found");
            return;
        }

        if (jwksValidation === 'true') {
            const jwksClient = require('jwks-rsa');
            const client = jwksClient({
                jwksUri: process.env.JWKS_URL
            });

            const getKey = (header) => {
                return new Promise((resolve, reject) => {
                    client.getSigningKey(header.kid, (err, key) => {
                        if (err) {
                            reject(err);
                        } else {
                            const signingKey = key.publicKey || key.rsaPublicKey;
                            resolve(signingKey);
                        }
                    });
                });
            };

            if (!token.startsWith("Bearer ")) {
                res.status(400).send("Invalid Token: it should start with 'Bearer'");
                return;
            }
            token = token.replace("Bearer ", "");

            try {
                const decoded = await new Promise((resolve, reject) => {
                    jwt.verify(token, async (header, callback) => {
                        try {
                            const key = await getKey(header);
                            callback(null, key);
                        } catch (err) {
                            callback(err);
                        }
                    }, (err, decoded) => {
                        if (err) {
                            reject(err);
                        } else {
                            resolve(decoded);
                        }
                    });
                });
                console.log(`Decoded JWT: ${JSON.stringify(decoded)}`);
                req.user = decoded;
            } catch (err) {
                console.log(err);
                res.status(401).send(err.message);
                return;
            }
        } else {
            if (!secretKey) {
                throw Error("secretKey Not found");
            }
            if (!token.startsWith("Bearer ")) {
                res.status(400).send("Invalid Token: it should start with 'Bearer'");
                return;
            }
            token = token.replace("Bearer ", "");
            try {
                const decoded = jwt.verify(token, secretKey);
                console.log(`Decoded JWT: ${JSON.stringify(decoded)}`);
                req.user = decoded;
            } catch (err) {
                console.log(err);
                res.status(401).send(err.message);
                return;
            }
        }

        if ('OPTIONS' == req.method) {
            res.sendStatus(200);
        }
        else {
            next();
        }
    } catch (e) {
        console.log("Error validate JWT", e);
        res.status(401).send(`Error on validate JWT: ${e}`);
    }
}

function actuatorRoute(req, res) {
    if (req.path === '/actuator') {
        const jsonMessage = `{
                        "_links": {
                            "self": {
                                "href": "http://${hostName}:${port}/actuator",
                                "templated": false
                            },
                            "health": {
                                "href": "http://${hostName}:${port}/actuator/health",
                                "templated": false
                            },
                            "info": {
                                "href": "http://${hostName}:${port}/actuator/info",
                                "templated": false
                            },
                            "prometheus": {
                                "href": "http://${hostName}:${port}/actuator/prometheus",
                                "templated": false
                            },
                            "metrics": {
                                "href": "http://${hostName}:${port}/actuator/metrics",
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

module.exports = { app: app };