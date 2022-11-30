from .database_setup import db


import datetime


class Product(db.Document):
    name = db.StringField(required=True, unique=True)
    quantity = db.IntField(required=True)
    category = db.StringField(required=True)
    createdDate = db.DateTimeField(default=datetime.datetime.utcnow)
    lastModifiedDate = db.DateTimeField(default=datetime.datetime.utcnow)
    createdByUser = db.StringField()
    lastModifiedByUser = db.StringField()
    price = db.DecimalField(required=True)
    currency = db.StringField(required=True)


class Receipt(db.Document):
    name = db.StringField(required=True)
    total = db.DecimalField(required=True)
    attachments = db.BinaryField()
    createdDate = db.DateTimeField(default=datetime.datetime.utcnow)
    lastModifiedDate = db.DateTimeField(default=datetime.datetime.utcnow)
    createdByUser = db.StringField()
    lastModifiedByUser = db.StringField()
    date = db.DateTimeField(required=True)
    products = db.ListField(db.ReferenceField(Product))
