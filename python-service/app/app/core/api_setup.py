from flask_restplus import Api

authorizations = {
    'apikey': {
        'type': 'apiKey',
        'in': 'header',
        'name': 'Authorization'
    }
}


def initialize_api(app):
    return Api(app=app, catch_all_404s=True, version='1.0', title='API - Products Service',
          description='Products Management', doc='/swagger-ui.html',
          default_label='products endpoints', default='products',
          authorizations=authorizations, security='apikey')