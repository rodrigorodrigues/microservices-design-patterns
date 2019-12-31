from .products import ProductsApi, ProductApi


def initialize_routes(api):
    api.add_resource(ProductsApi, '/api/products')
    api.add_resource(ProductApi, '/api/products/<id>')
