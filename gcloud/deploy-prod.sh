#!/bin/bash

set -e

for i in $(echo $IMAGES_TO_BUILD | tr ";" "\n")
do
  echo "Deploying $i";
  if [ "$i" == "react-webapp" ]; then
    echo "Building React Web App..."
    docker build -t eu.gcr.io/${GCP_PROJECT_ID}/${i}:$TRAVIS_COMMIT -f react-webapp/Dockerfile .
  elif [ "$i" == "nodejs-service" ]; then
    echo "Building NodeJS Service..."
    docker build -t eu.gcr.io/${GCP_PROJECT_ID}/${i}:$TRAVIS_COMMIT -f nodejs-service/Dockerfile .
  fi

  echo $GCLOUD_SERVICE_KEY_PROD | base64 --decode -i > ${HOME}/gcloud-service-key.json
  gcloud auth activate-service-account --key-file ${HOME}/gcloud-service-key.json

  gcloud --quiet config set project $GCP_PROJECT_ID
  #gcloud --quiet config set container/cluster $CLUSTER
  #gcloud --quiet config set compute/zone ${ZONE}
  gcloud --quiet container clusters get-credentials $CLUSTER

  echo "Pushing docker image $DOCKER_IMAGE..."
  gcloud docker -- push eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}

  yes | gcloud beta container images add-tag eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:latest

  kubectl config view
  kubectl config current-context

  kubectl set image deployment/${NGINX_DEPLOYMENT} ${NGINX_CONTAINER}=eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT
done