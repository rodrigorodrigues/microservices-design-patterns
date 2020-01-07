import datetime
import logging.config

from autologging import logged, traced
from flask import Response, request, make_response, jsonify
from flask_jwt_extended import get_jwt_identity
from flask_restplus import Resource

from app.core.api import api, productModel
from app.jwt_custom_decorator import admin_required
from app.model.models import Product

log = logging.getLogger(__name__)

ns = api.namespace('api/products', description='Product operations')


@traced(log)
@logged(log)
@ns.route('')
class ProductsApi(Resource):
    findAllPermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_READ', 'ROLE_PRODUCTS_CREATE',
                                                            'ROLE_PRODUCTS_SAVE', 'ROLE_PRODUCTS_DELETE'])

    createPermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_CREATE'])

    """Return list of products"""

    @findAllPermissions
    @ns.doc(description='List of products', responses={
        200: 'List of products',
        400: 'Validation Error',
        401: 'Unauthorized',
        403: 'Forbidden',
        500: 'Unexpected Error'
    })
    def get(self):
        log.debug('Get all products')
        products = Product.objects().to_json()
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
                                                                           'ROLE_PRODUCTS_SAVE'])
    savePermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_SAVE'])

    deletePermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_DELETE'])

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
    def delete(self, id):
        user_id = get_jwt_identity()
        movie = Product.objects.get(id=id)
        movie.delete()
        return make_response(jsonify(msg='Deleted product id: ' + id), 200)

    @findByIdPermissions
    def get(self, id):
        product = Product.objects.get(id=id).to_json()
        return Response(product, mimetype="application/json", status=200)
