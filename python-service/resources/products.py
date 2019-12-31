import datetime

from flask import Response, request, jsonify
from flask_jwt_extended import get_jwt_identity
from flask_restplus import Resource

from database.models import Products
from resources.jwt_custom_decorator import admin_required


class ProductsApi(Resource):
    findAllPermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_READ', 'ROLE_PRODUCTS_CREATE',
                                                 'ROLE_PRODUCTS_SAVE',
                                                 'ROLE_PRODUCTS_DELETE'])

    createPermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_CREATE'])

    @findAllPermissions
    def get(self):
        query = Products.objects()
        movies = Products.objects().to_json()
        return Response(movies, mimetype="application/json", status=200)

    @createPermissions
    def post(self):
        user_id = get_jwt_identity()
        body = request.get_json()
        product = Products(**body)
        product.createdByUser = user_id
        product.save()
        return Response(product.to_json(), mimetype="application/json", status=200)


class ProductApi(Resource):
    findByIdPermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_READ'
                                                                           'ROLE_PRODUCTS_SAVE'])
    savePermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_SAVE'])

    deletePermissions = lambda f: admin_required(f, roles=['ROLE_ADMIN', 'ROLE_PRODUCTS_DELETE'])

    @savePermissions
    def put(self, id):
        user_id = get_jwt_identity()
        product = Products.objects.get(id=id)
        body = request.get_json()
        product.lastModifiedDate = datetime.datetime.utcnow()
        product.lastModifiedByUser = user_id
        product.update(**body)
        return Response(Products.objects.get(id=id).to_json(), mimetype="application/json", status=200)

    @deletePermissions
    def delete(self, id):
        user_id = get_jwt_identity()
        movie = Products.objects.get(id=id)
        movie.delete()
        return Response(jsonify(msg='Delete product id: {id'), mimetype="application/json", status=200)

    @findByIdPermissions
    def get(self, id):
        product = Products.objects.get(id=id).to_json()
        return Response(product, mimetype="application/json", status=200)
