import logging.config
import re
from dataclasses import dataclass
from typing import List

import cv2
import iso4217parse
import pytesseract
from pytesseract import Output

log = logging.getLogger(__name__)

re_total = re.compile(r"total to pay|card sales|sub total|sub-total|total|sale total", re.IGNORECASE)
re_date = re.compile(r"(?=\d{2}(?:\d{2})?-\d{1,2}-\d{1,2}\b)")
re_non_decimal_number = re.compile("[^0-9.,]")
re_non_words = re.compile(r"\W+")
re_decimal_number = re.compile(r"(.)*(\d[\.,\s]\d|[\.,\s]\d)")


@dataclass
class Product:
    name: str = None
    quantity: int = None
    category: str = None
    total_price: float = None
    quantity_price: float = None
    currency: str = None


@dataclass
class Receipt:
    name: str = None
    total: float = None
    attachments: bytearray = None
    products: List[Product] = None


def ocr_core(filename):
    """
    This function will handle the core OCR processing of images.
    """
    img = _convert_image_to_bytes(filename)

    data = pytesseract.image_to_string(img)
    log.debug('Data: %s', data)
    receipt = Receipt()
    products = []
    previous_line = None
    iso4217 = None
    for _line in data.split('\n')[1:]:
        if len(_line) > 2 and (not _line.isspace()):
            _line = _line.replace(". ", ".").replace(" .", ".")
            if receipt.name is None and not _line.__contains__('CASH SALE'):
                receipt.name = _line
            elif re_total.match(_line) and receipt.total is None:
                receipt.total = float(re_non_decimal_number.sub("", _line))
            elif re_date.match(_line) and receipt.date is None:
                receipt.date = _line
                break
            elif re_decimal_number.match(_line) and receipt.total is None:
                product = Product()
                if iso4217 is None:
                    iso4217 = iso4217parse.parse(_line)
                currency_name = " "
                if iso4217:
                    currency_symbol = iso4217[0].symbols[0]
                    currency_name = iso4217[0].alpha3
                    product.currency = currency_symbol
                line_split = _line.split(currency_name)
                if len(line_split) == 2:
                    product.name = line_split[0].strip()
                    product.quantity = 1
                    product.total_price = float(re_non_decimal_number.sub("", line_split[1].replace(" ", ".")))
                    product.quantity_price = product.total_price
                else:
                    _process_quantity_product(product, previous_line, currency_symbol, currency_name, _line)

                products.append(product)
                receipt.products = products
            previous_line = _line
    log.debug('Receipt: %s', receipt)
    return receipt


def _convert_image_to_bytes(filename):
    img = cv2.imread(filename)
    d = pytesseract.image_to_data(img, output_type=Output.DICT)
    n_boxes = len(d['level'])
    for i in range(n_boxes):
        (x, y, w, h) = (d['left'][i], d['top'][i], d['width'][i], d['height'][i])
    img = cv2.rectangle(img, (x, y), (x + w, y + h), (0, 0, 255), 2)
    return img


def _process_quantity_product(product, previous_line, currency_symbol, currency_name, _line):
    _line = re.compile(f"{currency_symbol}|{currency_name}", re.IGNORECASE).sub("", _line)
    total_product_price = None
    quantity_price = None
    for line in reversed(_line.split(' ')):
        line = re_non_decimal_number.sub("", line)
        if line:
            if total_product_price is None:
                total_product_price = line
            elif quantity_price is None and not line.__contains__('.'):
                total_product_price = line + total_product_price
            else:
                if quantity_price:
                    quantity_price = line + quantity_price
                else:
                    quantity_price = line
                if float(total_product_price) % float(quantity_price) == 0:
                    break

    if total_product_price and quantity_price:
        product.name = previous_line
        product.quantity = float(total_product_price) / float(quantity_price)
        product.total_price = float(total_product_price)
        product.quantity_price = float(quantity_price)
