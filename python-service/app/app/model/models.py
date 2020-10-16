from core.database_setup import db
import datetime


class   Product(db.Document):
    name = db.StringField(required=True, unique=True)
    quantity = db.IntField(required=True)
    category = db.StringField(required=True)
    createdDate = db.DateTimeField(default=datetime.datetime.utcnow)
    lastModifiedDate = db.DateTimeField(default=datetime.datetime.utcnow)
    createdByUser = db.StringField()
    lastModifiedByUser = db.StringField()
