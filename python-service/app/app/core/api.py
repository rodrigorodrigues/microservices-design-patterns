from flask_restplus import Api, fields

from app.main import app

authorizations = {
    'apikey': {
        'type': 'apiKey',
        'in': 'header',
        'name': 'Authorization'
    }
}

api = Api(app=app, catch_all_404s=True, version='1.0', title='API - Products Service',
          description='Products Management', doc='/swagger-ui.html',
          default_label='products endpoints', default='products',
          authorizations=authorizations, security='apikey')

productModel = api.model('Product', {
    'name': fields.String(required=True, description='Name'),
    'quantity': fields.Integer(required=True, description='Quantity'),
    'category': fields.String(required=True, description='Category Name'),
})
