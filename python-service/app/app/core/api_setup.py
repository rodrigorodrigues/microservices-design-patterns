from flask import url_for
from flask_restx import Api


class CustomApi(Api):
    @property
    def specs_url(self):
        """
        The Swagger specifications absolute url (ie. `swagger.json`)

        :rtype: str
        """
        return url_for(self.endpoint('specs'), _external=False)


authorizations = {
    'apikey': {
        'type': 'apiKey',
        'in': 'header',
        'name': 'Authorization'
    }
}


def initialize_api(app):
    return CustomApi(app=app, catch_all_404s=True, version='1.0', title='API - Products Service',
                     description='Products Management', doc='/swagger-ui.html',
                     default_label='products endpoints', default='products',
                     authorizations=authorizations, security='apikey')