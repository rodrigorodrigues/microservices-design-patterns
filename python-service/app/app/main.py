import logging.config
import os
import sys

from flask import Flask
from flask import jsonify, make_response
from flask_jwt_extended import JWTManager
from werkzeug.serving import run_simple

from app.core.database import initialize_db

app = Flask(__name__)
app.config.from_envvar('ENV_FILE_LOCATION')
for v in os.environ:
    env = os.getenv(v)
    app.config[env] = env
app.config['MONGODB_SETTINGS'] = {
    'host': app.config['MONGODB_URI']
}
jwt = JWTManager(app)

log = logging.getLogger(__name__)


@app.errorhandler(Exception)
def handle_root_exception(error):
    """Return a custom message and 400 or 500 status code"""
    log.exception(error)
    if hasattr(error, 'errors'):
        return make_response(jsonify(error=str(error.errors)), 400)
    return make_response(jsonify(error=str(error)), 500)


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


def init_app():
    logging.basicConfig(
        format="%(levelname)s [%(name)s %(funcName)s] %(message)s",
        level=app.config['LOG_LEVEL'],
        stream=sys.stdout
    )
    from app.api.products import ns
    from app.core.api import api
    api.add_namespace(ns)
    api.init_app(app)
    from app.core.spring_cloud import initialize_spring_cloud_client, dispatcher
    initialize_spring_cloud_client()
    initialize_db(app)
    run_simple(hostname="0.0.0.0", port=app.config['SERVER_PORT'], application=dispatcher)


if __name__ == "__main__":
    init_app()

    app.run(debug=app.config['DEBUG'], host='0.0.0.0', port=app.config['SERVER_PORT'])
