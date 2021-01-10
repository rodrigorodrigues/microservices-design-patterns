#FROM tiangolo/uwsgi-nginx-flask:python3.8-alpine
FROM tiangolo/uwsgi-nginx-flask:python3.8

ARG PORT

MAINTAINER Rodrigo Rodrigues "rodrigorodriguesweb@gmail.com"

ENV LISTEN_PORT $PORT

ENV NGINX_MAX_UPLOAD 10m

EXPOSE $PORT

RUN apt-get update && apt-get install ffmpeg libsm6 libxext6 tesseract-ocr libtesseract-dev -y

#RUN apk --update add --no-cache tesseract-ocr-dev

#RUN apk add build-base python-dev py-pip jpeg-dev zlib-dev

#ENV LIBRARY_PATH=/lib:/usr/lib

COPY ./app /app

WORKDIR /app

RUN find ./app -type f -name "*.py" -exec sed -i 's/from core./from .core./g' {} +

RUN find ./app -type f -name "*.py" -exec sed -i 's/from model./from .model./g' {} +

RUN find ./app -type f -name "*.py" -exec sed -i 's/from jwt_custom_decorator/from .jwt_custom_decorator/g' {} +

RUN echo "\nJWT_DECODE_ALGORITHMS = ['RS256']" >> ./app/.env

RUN pip install --upgrade pip && pip install --no-cache-dir -r ./app/requirements.txt
