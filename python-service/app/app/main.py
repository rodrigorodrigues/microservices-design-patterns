import datetime
import logging.config
import os

import sys
from autologging import traced, logged
from core.api_setup import initialize_api
from core.consul_setup import initialize_consul_client, initialize_dispatcher
from core.kubernetes_setup import initialize_kubernetes_client
from model.database_setup import initialize_db
from core.ocr_core import ocr_core
from flask import Flask, request, Response
from flask import jsonify, make_response
from flask_jwt_extended import JWTManager, get_jwt_identity
from flask_opentracing import FlaskTracing
from flask_restx import fields, Resource
from jaeger_client import Config
from jwt_custom_decorator import admin_required
from model.models import Product
from werkzeug.serving import run_simple
from werkzeug.datastructures import FileStorage

app = Flask(__name__)
if not os.getenv('ENV_FILE_LOCATION'):
    os.environ["ENV_FILE_LOCATION"] = ".env"
app.config.from_envvar('ENV_FILE_LOCATION')
app.debug = app.config['DEBUG']

for v in os.environ:
    env = os.getenv(v)
    if v == 'SERVER_PORT':
        env = int(env)
    app.config[v] = env

app.config['MONGODB_SETTINGS'] = {
    'host': app.config['MONGODB_URI'],
    'connect': False
}
jwt = JWTManager(app)

log = logging.getLogger(__name__)

logging.basicConfig(
    format="%(levelname)s [%(name)s %(funcName)s] %(message)s",
    level=app.config['LOG_LEVEL'],
    stream=sys.stdout
)

if str(app.config['SPRING_PROFILES_ACTIVE']).__contains__('kubernetes'):
    initialize_kubernetes_client(app)
else:
    initialize_consul_client(app)

initialize_db(app)
api = initialize_api(app)

ns = api.namespace('api/products', description='Product operations')
nsReceipt = api.namespace('api/receipts', description='Receipt operations')

productModel = api.model('Product', {
    'name': fields.String(required=True, description='Name'),
    'quantity': fields.Integer(required=True, description='Quantity'),
    'category': fields.String(required=True, description='Category Name'),
})

upload_parser = api.parser()
upload_parser.add_argument('file', location='files',
                           type=FileStorage, required=True)

# Create configuration object with enabled logging and sampling of all requests.
config = Config(config={'sampler': {'type': 'const', 'param': 1},
                        'logging': True,
                        'local_agent':
                        # Also, provide a hostname of Jaeger instance to send traces to.
                            {'reporting_host': app.config['JAEGER_HOST']}},
                # Service name can be arbitrary string describing this particular web service.
                service_name=app.config['APP_NAME'])
jaeger_tracer = config.initialize_tracer()
tracing = FlaskTracing(tracer=jaeger_tracer, app=app)


createPermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_CREATE', 'SCOPE_openid'])


ALLOWED_EXTENSIONS = app.config['ALLOWED_EXTENSIONS']


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@traced(log)
@logged(log)
@nsReceipt.route('')
class ReceiptsApi(Resource):

    @createPermissions
    @ns.expect(upload_parser)
    @tracing.trace()
    def post(self):
        max()
        args = upload_parser.parse_args()
        file = args['file']
        # if user does not select file, browser also
        # submit a empty part without filename
        if file.filename == '':
            return make_response(jsonify(msg='No file selected'), 400)

        if not allowed_file(file.filename):
            return make_response(jsonify(msg='file is invalid:\nValid extensions are: '+''.join(ALLOWED_EXTENSIONS)), 400)

        upload_folder = app.config['UPLOAD_FOLDER']

        if not os.path.exists(upload_folder):
            os.makedirs(upload_folder)

        file.save(os.path.join(upload_folder, file.filename))

        # call the OCR function on it
        extracted_text = ocr_core(upload_folder + file.filename)

        return make_response(jsonify(msg=extracted_text), 200)


@traced(log)
@logged(log)
@ns.route('')
class ProductsApi(Resource):
    findAllPermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_READ', 'ROLE_PRODUCTS_CREATE',
                                                            'ROLE_PRODUCTS_SAVE', 'ROLE_PRODUCTS_DELETE', 'SCOPE_openid'])

    """Return list of products"""

    @findAllPermissions
    @ns.doc(description='List of products', responses={
        200: 'List of products',
        400: 'Validation Error',
        401: 'Unauthorized',
        403: 'Forbidden',
        500: 'Unexpected Error'
    })
    @tracing.trace()
    def get(self):
        page = request.args.get("page", 0)
        size = request.args.get("size", 10)
        log.debug(f'Get all products - page: {page}\t size: {size}')
        # products = Product.objects().to_json()
        products = Product.objects.paginate(page=page, per_page=size).to_json()
        return Response(products, mimetype="application/json", status=200)

    """Create new product"""

    @createPermissions
    @ns.doc(description='Create product', responses={
        201: 'Created',
        400: 'Validation Error',
        401: 'Unauthorized',
        403: 'Forbidden',
        500: 'Unexpected Error'
    })
    @ns.expect(productModel)
    @tracing.trace()
    def post(self):
        user_id = get_jwt_identity()
        body = request.get_json()
        product = Product(**body)
        product.createdByUser = user_id
        product.save()
        return Response(product.to_json(), mimetype="application/json", status=201)


@ns.route('/<string:id>')
class ProductApi(Resource):
    findByIdPermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_READ'
                                                                           'ROLE_PRODUCTS_SAVE', 'SCOPE_openid'])


    savePermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_SAVE', 'SCOPE_openid'])


    deletePermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_DELETE', 'SCOPE_openid'])

    """Update product"""

    @savePermissions
    @ns.doc(params={'id': 'An ID'}, description='Update product', responses={
        200: 'Updated Successfully',
        400: 'Validation Error',
        401: 'Unauthorized',
        403: 'Forbidden',
        500: 'Unexpected Error'
    })
    @ns.expect(productModel)
    @tracing.trace()
    def put(self, id):
        user_id = get_jwt_identity()
        product = Product.objects.get(id=id)
        body = request.get_json()
        product.lastModifiedDate = datetime.datetime.utcnow()
        product.lastModifiedByUser = user_id
        product.update(**body)
        return Response(Product.objects.get(id=id).to_json(), mimetype="application/json", status=200)

    """Delete product"""

    @deletePermissions
    @ns.doc(description='Delete product', responses={
        200: 'Deleted Successfully',
        400: 'Validation Error',
        401: 'Unauthorized',
        403: 'Forbidden',
        500: 'Unexpected Error'
    })
    @tracing.trace()
    def delete(self, id):
        user_id = get_jwt_identity()
        movie = Product.objects.get(id=id)
        movie.delete()
        return make_response(jsonify(msg='Deleted product id: ' + id), 200)

    @findByIdPermissions
    @tracing.trace()
    def get(self, id):
        product = Product.objects.get(id=id).to_json()
        return Response(product, mimetype="application/json", status=200)


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


server_port = app.config['SERVER_PORT']


@app.route('/actuator/info')
def actuator_info():
    return jsonify({})


@app.route('/actuator')
def actuator_index():
    port = server_port
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


api.add_namespace(ns)
debug_flag = app.config['DEBUG']

if __name__ == "__main__":
    run_simple(hostname="0.0.0.0", port=server_port, application=initialize_dispatcher(app), use_debugger=debug_flag)
