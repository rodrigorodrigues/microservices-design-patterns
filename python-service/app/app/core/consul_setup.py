import base64
import logging.config

import yaml
from flask_consulate import Consul
from flask_prometheus_metrics import register_metrics
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware

log = logging.getLogger(__name__)


def initialize_dispatcher(app):
    # Plug metrics WSGI app to your main app with dispatcher
    return DispatcherMiddleware(app.wsgi_app, {"/actuator/prometheus": make_wsgi_app()})


def initialize_consul_client(app):
    server_port = app.config['SERVER_PORT']

    app_name = app.config['APP_NAME']

    profile = app.config['SPRING_PROFILES_ACTIVE']

    # Consul
    # This extension should be the first one if enabled:
    consul = Consul(app=app)
    # Fetch the configuration:
    consul.apply_remote_config(namespace=f'config/application,{profile}/data')
    # Register Consul service:
    consul.register_service(
        name=app_name,
        interval='30s',
        tags=[app_name],
        port=server_port,
        httpcheck="http://localhost:" + str(server_port) + "/actuator/health"
    )

    if profile != 'prod':
        jwt_secret = ""
        for data in yaml.load_all(app.config[''], Loader=yaml.BaseLoader):
            try:
                jwt_secret = data['security']['oauth2']['resource']['jwt']['keyValue']
                break
            except Exception:
                log.warning("Not found jwt_secret")

        if jwt_secret == "":
            raise Exception("jwt_secret not found")
        log.debug('Jwt Secret: %s', jwt_secret)
        app.config['JWT_SECRET_KEY'] = base64.b64decode(jwt_secret)
        app.config['SECRET_KEY'] = app.config['JWT_SECRET_KEY']

    else:
        app.config['JWT_PUBLIC_KEY'] = open(app.config['JWT_PUBLIC_KEY'], "r").read()

    log.debug('Config environment: %s', app.config)

    # provide app's version and deploy environment/config name to set a gauge metric
    register_metrics(app, app_version="v0.1.2", app_config="staging")
