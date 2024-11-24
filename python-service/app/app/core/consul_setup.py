import json
import logging.config
import os

import consul
import requests
import yaml
from flask_consulate import Consul
from flask_consulate.decorators import with_retry_connections
from flask_prometheus_metrics import register_metrics
from jwt.algorithms import RSAAlgorithm
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware

log = logging.getLogger(__name__)


@with_retry_connections()
def apply_remote_config(namespace=None, app=None):
    """
    Applies all config values defined in consul's kv store to self.app.

    There is no guarantee that these values will not be overwritten later
    elsewhere.

    :param namespace: kv namespace/directory. Defaults to
            DEFAULT_KV_NAMESPACE
    :param app: flask.Flask application instance
    :return: None
    """
    consul_array = app.config['CONSUL_URL'].split(':')
    consul_scheme = consul_array[0]
    consul_host = consul_array[1].replace('/', '')
    consul_port = consul_array[2]

    consul_client = consul.Consul(scheme=consul_scheme, host=consul_host, port=consul_port)

    if namespace is None:
        namespace = "config/{service}/{environment}/".format(
            service=os.environ.get('SERVICE', 'generic_service'),
            environment=os.environ.get('ENVIRONMENT', 'generic_environment')
        )

    # Fetch the configuration:
    k, v = consul_client.kv.get(key=namespace)
    k = k.replace(namespace, '')
    try:
        data = json.loads(json.loads(json.dumps(str(v).replace("'", "\"").replace("b\"", "\""))))
        app.config[''] = data['Value']
    except (TypeError, ValueError):
        app.logger.warning("Couldn't de-serialize {} to json, using raw value".format(v))
        app.config[''] = v

    msg = "Set {k}={v} from consul kv '{ns}'".format(
        k=k,
        v=v,
        ns=namespace,
    )
    app.logger.debug(msg)


def initialize_dispatcher(app):
    # Plug metrics WSGI app to your main app with dispatcher
    return DispatcherMiddleware(app.wsgi_app, {"/actuator/prometheus": make_wsgi_app()})


def initialize_consul_client(app):
    server_port = app.config['SERVER_PORT']

    app_name = app.config['APP_NAME']

    profile = app.config['SPRING_PROFILES_ACTIVE']

    hostname = app.config['HOSTNAME']

    # Consul
    # This extension should be the first one if enabled:
    consul = Consul(app=app)
    apply_remote_config(namespace=f'config/application,{profile}/data', app=app)
    # consul.apply_remote_config(namespace=f'config/application,{profile}/data')
    # Register Consul service:
    # consul_client.agent.service.register(name=app_name,
    #                                     tags=[app_name],
    #                                     port=server_port,
    #                                     interval='30s',
    #                                     check=hostname + ":" + str(server_port) + "/actuator/health")
    consul.register_service(
        name=app_name,
        interval='30s',
        tags=[app_name],
        port=server_port,
        httpcheck=hostname + ":" + str(server_port) + "/actuator/health"
    )

    if profile != 'prod':
        if app.config['JWKS_URL'] is not None:
            # retrieve master openid-configuration endpoint for issuer realm
            jwks_url = requests.get(app.config['JWKS_URL']).json()

            # retrieve first jwk entry from jwks_uri endpoint and use it to construct the
            # RSA public key
            app.config["JWT_PUBLIC_KEY"] = RSAAlgorithm.from_jwk(
                json.dumps(jwks_url["keys"][0])
            )
        else:

            jwt_secret = ""
            for data in yaml.load_all(app.config[''], Loader=yaml.BaseLoader):
                try:
                    jwt_secret = data['com']['microservice']['authentication']['jwt']['keyValue']
                    break
                except Exception:
                    log.warning("Not found jwt_secret")

            if jwt_secret == "":
                raise Exception("jwt_secret not found")
            log.debug('Jwt Secret: %s', jwt_secret)
            app.config['JWT_SECRET_KEY'] = jwt_secret

    elif app.config['JWKS_URL'] is not None:
        # retrieve master openid-configuration endpoint for issuer realm
        jwks_url = requests.get(app.config['JWKS_URL']).json()

        # retrieve first jwk entry from jwks_uri endpoint and use it to construct the
        # RSA public key
        app.config["JWT_PUBLIC_KEY"] = RSAAlgorithm.from_jwk(
            json.dumps(jwks_url["keys"][0])
        )

    else:
        app.config['JWT_PUBLIC_KEY'] = open(app.config['JWT_PUBLIC_KEY'], "r").read()

    log.debug('Config environment: %s', app.config)

    # provide app's version and deploy environment/config name to set a gauge metric
    register_metrics(app, app_version="v0.1.2", app_config="staging")
