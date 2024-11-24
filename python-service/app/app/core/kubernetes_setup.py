import logging.config
import json

import yaml
import requests
from kubernetes import client, config
from flask_prometheus_metrics import register_metrics
from jwt.algorithms import RSAAlgorithm

log = logging.getLogger(__name__)


def initialize_kubernetes_client(app):
    config.load_incluster_config()
    v1 = client.CoreV1Api()
    config_map = v1.read_namespaced_config_map(name="python-service", namespace='default', pretty=True)
    log.debug('config_map: %s', config_map)
    profile = app.config['SPRING_PROFILES_ACTIVE']
    if not str(profile).__contains__('prod'):
        jwt_secret = ""
        try:
            for data in yaml.load_all(config_map, Loader=yaml.BaseLoader):
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
