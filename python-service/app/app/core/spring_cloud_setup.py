import base64
import logging.config

import py_eureka_client.eureka_client as eureka_client
from config import spring
from flask_prometheus_metrics import register_metrics
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware

log = logging.getLogger(__name__)


def initialize_dispatcher(app):
    # Plug metrics WSGI app to your main app with dispatcher
    return DispatcherMiddleware(app.wsgi_app, {"/actuator/prometheus": make_wsgi_app()})


def initialize_spring_cloud_client(app):

    # The following code will register server to eureka server and also start to send heartbeat every 30 seconds
    eureka_client.init(eureka_server=app.config['EUREKA_SERVER'],
                       app_name="python-service",
                       instance_port=app.config['SERVER_PORT'])

    address = app.config["SPRING_CLOUD_CONFIG_URI"]

    profile = app.config['SPRING_PROFILES_ACTIVE']

    app_name = app.config['APP_NAME']

    config_client = spring.ConfigClient(
        app_name=app_name,
        url="{address}/{app_name}/{profile}.json",
        profile=profile,
        branch=None,
        address=address
    )
    config_client.url = config_client.url[:-5]
    config_client.get_config(headers={'X-Encrypt-Key': app.config['X_ENCRYPT_KEY']})

    if profile != 'prod':
        try:
            jwt_secret = config_client.config['propertySources'][0]['source']['configuration.jwt.base64-secret']
        except Exception:
            jwt_secret = config_client.config['propertySources'][1]['source']['configuration.jwt.base64-secret']
        log.debug('Jwt Secret: %s', jwt_secret)
        app.config['JWT_SECRET_KEY'] = base64.b64decode(jwt_secret)

    else:
        app.config['JWT_PUBLIC_KEY'] = open(app.config['JWT_PUBLIC_KEY'], "r").read()

    log.debug('Config environment: %s', app.config)

    # provide app's version and deploy environment/config name to set a gauge metric
    register_metrics(app, app_version="v0.1.2", app_config="staging")
