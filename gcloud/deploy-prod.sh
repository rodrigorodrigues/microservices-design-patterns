#!/bin/bash

set -e

for DOCKER_IMAGE in $(echo $IMAGES_TO_BUILD | tr ";" "\n")
do
  echo "Preparing deploying for docker image $DOCKER_IMAGE";
  if [ "$DOCKER_IMAGE" == "react-webapp" ]; then
    echo "Building Docker React Web App Image..."
    docker build --quiet -t eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT -f ../react-webapp/Dockerfile
  elif [ "$DOCKER_IMAGE" == "week-menu-api" ]; then
    echo "Building Docker NodeJS Service Image..."
    docker build --quiet -t eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT -f ../nodejs-service/Dockerfile
  fi

  echo $GCLOUD_SERVICE_KEY_PROD | base64 --decode -i > ${HOME}/gcloud-service-key.json
  gcloud auth activate-service-account --key-file ${HOME}/gcloud-service-key.json

  gcloud --quiet config set project $GCP_PROJECT_ID
  #gcloud --quiet config set container/cluster $CLUSTER
  #gcloud --quiet config set compute/zone ${ZONE}
  #gcloud --quiet container clusters get-credentials $CLUSTER

  echo "Pushing docker image $DOCKER_IMAGE..."
  gcloud docker -- push eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}

  yes | gcloud beta container images add-tag eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:latest

  kubectl config view
  kubectl config current-context

  echo "Deploy new docker image $DOCKER_IMAGE..."
  kubectl rollout restart deployment ${DOCKER_IMAGE}
done