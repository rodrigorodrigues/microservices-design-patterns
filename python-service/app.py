from flask import Flask, jsonify, make_response
from os import environ

from mongoengine import ValidationError, OperationError
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware
from werkzeug.serving import run_simple
from flask_prometheus_metrics import register_metrics
from flask_jwt_extended import JWTManager
from database.db import initialize_db
from flask_restplus import Api
import base64

from resources.routes import initialize_routes

import py_eureka_client.eureka_client as eureka_client

app = Flask(__name__)
app.config['MONGODB_SETTINGS'] = {
    'host': 'mongodb://localhost/docker'
}
app.config.from_envvar('ENV_FILE_LOCATION')
app.config['JWT_SECRET_KEY'] = base64.b64decode(app.config['JWT_SECRET_KEY'])

#
#
# def price(id):
#     r = requests.get('http://price/{}'.format(id))
#     return r.json()
#
#
# @jwt_required
# @app.route('/products')
# def products():
#     return jsonify(PRODUCTS)
#
#
# @app.route('/products/<id>')
# def product(id):
#     p = PRODUCTS[id].copy()
#     p['cost'] = price(id)
#     return jsonify(p)
#
#


@app.errorhandler(Exception)
def handle_root_exception(error):
    """Return a custom message and 400 status code"""
    if hasattr(error, 'errors'):
        return make_response(jsonify(error=str(error.errors)), 400)
    return make_response(jsonify(error=str(error)), 400)

@app.route('/actuator/health')
def health():
    return jsonify({'status': 'OK'})


@app.route('/actuator')
def actuator_index():
    port = app.config['SERVER_PORT']
    actuator = {
        "_links": {
            "self": {
                "href": "http://localhost:" + str(port) + "/actuator",
                "templated": False
            },
            "health": {
                "href": "http://localhost:" + str(port) + "/actuator/health",
                "templated": False
            },
            "info": {
                "href": "http://localhost:" + str(port) + "/actuator/info",
                "templated": False
            },
            "prometheus": {
                "href": "http://localhost:" + str(port) + "/actuator/prometheus",
                "templated": False
            },
            "metrics": {
                "href": "http://localhost:" + str(port) + "/actuator/metrics",
                "templated": False
            }
        }
    }
    return jsonify(actuator)


if __name__ == "__main__":
    # The flowing code will register your server to eureka server and also start to send heartbeat every 30 seconds
    eureka_client.init(eureka_server=app.config['EUREKA_SERVER'],
                       app_name="python-service",
                       instance_port=app.config['SERVER_PORT'])
    # provide app's version and deploy environment/config name to set a gauge metric
    register_metrics(app, app_version="v0.1.2", app_config="staging")

    # Plug metrics WSGI app to your main app with dispatcher
    dispatcher = DispatcherMiddleware(app.wsgi_app, {"/actuator/prometheus": make_wsgi_app()})

    initialize_db(app)

    api = Api(app, catch_all_404s=True)

    jwt = JWTManager(app)

    initialize_routes(api)

    run_simple(hostname="localhost", port=app.config['SERVER_PORT'], application=dispatcher)

    app.run(debug=True, host='0.0.0.0', port=app.config['SERVER_PORT'])
