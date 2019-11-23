#!/bin/bash

set -e

echo "Images to build: $IMAGES_TO_BUILD"

for DOCKER_IMAGE in $(echo $IMAGES_TO_BUILD | tr ";" "\n")
do
  echo "Preparing to deploy docker image $DOCKER_IMAGE"
  if [ "$DOCKER_IMAGE" == "react-webapp" ]; then
    echo "Building Docker React Web App Image..."
    docker build --quiet -t eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT ../react-webapp
  elif [ "$DOCKER_IMAGE" == "week-menu-api" ]; then
    echo "Building Docker NodeJS Service Image..."
    docker build --quiet -t eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT ../nodejs-service
  else
    echo "Tagging docker image $DOCKER_IMAGE..."
    docker tag ${DOCKER_IMAGE}:latest eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT
  fi

  echo $GCLOUD_SERVICE_KEY_PROD | base64 --decode -i > ${HOME}/gcloud-service-key.json
  gcloud auth activate-service-account --key-file ${HOME}/gcloud-service-key.json

  gcloud --quiet config set project $GCP_PROJECT_ID
  #gcloud --quiet config set container/cluster $CLUSTER
  #gcloud --quiet config set compute/zone ${ZONE}
  #gcloud --quiet container clusters get-credentials $CLUSTER

  echo "Pushing docker image $DOCKER_IMAGE..."
  gcloud docker -- push eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}

  echo "Adding tag image to latest container image $DOCKER_IMAGE..."
  yes | gcloud beta container images add-tag eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:latest

  kubectl config view
  kubectl config current-context

  echo "Deploying new docker image $DOCKER_IMAGE..."
  kubectl patch deployment ${DOCKER_IMAGE} -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"date\":\"`date +'%s'`\"}}}}}"
done