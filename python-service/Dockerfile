FROM tiangolo/uwsgi-nginx-flask:python3.7

ARG PORT

MAINTAINER Rodrigo Rodrigues "rodrigorodriguesweb@gmail.com"

ENV LISTEN_PORT $PORT

EXPOSE $PORT

COPY ./app /app

WORKDIR /app

RUN pip install --upgrade pip && pip install --no-cache-dir -r ./app/requirements.txt