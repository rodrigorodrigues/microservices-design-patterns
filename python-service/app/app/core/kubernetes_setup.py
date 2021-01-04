import logging.config

import yaml
from kubernetes import client, config
from flask_prometheus_metrics import register_metrics

log = logging.getLogger(__name__)


def initialize_kubernetes_client(app):
    config.load_kube_config()
    v1 = client.CoreV1Api()
    field_selector = "app=python-service"
    config_map = v1.list_namespaced_config_map(namespace='default', pretty=True, field_selector=field_selector)
    log.debug('config_map: %s', config_map)
    profile = app.config['SPRING_PROFILES_ACTIVE']
    if profile != 'prod':
        jwt_secret = ""
        for data in yaml.load_all(config_map, Loader=yaml.BaseLoader):
            try:
                jwt_secret = data['com']['microservice']['authentication']['jwt']['keyValue']
                break
            except Exception:
                log.warning("Not found jwt_secret")

        if jwt_secret == "":
            raise Exception("jwt_secret not found")
        log.debug('Jwt Secret: %s', jwt_secret)
        app.config['JWT_SECRET_KEY'] = jwt_secret

    else:
        app.config['JWT_PUBLIC_KEY'] = open(app.config['JWT_PUBLIC_KEY'], "r").read()

    log.debug('Config environment: %s', app.config)

    # provide app's version and deploy environment/config name to set a gauge metric
    register_metrics(app, app_version="v0.1.2", app_config="staging")
