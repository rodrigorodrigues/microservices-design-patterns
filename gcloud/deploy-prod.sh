#!/bin/bash

set -e

BUILD_NEW_DOCKER_IMAGE=false
BUILD_REACT_WEBAPP_IMAGE=false
BUILD_REACT_WEBAPP_JHIPSTER_IMAGE=false
BUILD_NODE_IMAGE=false
BUILD_AUTHENTICATION_SERVICE_IMAGE=false
BUILD_USER_SERVICE_IMAGE=false
BUILD_PERSON_SERVICE_IMAGE=false
BUILD_KOTLIN_SERVICE_IMAGE=false
DEPLOY_FILES="$(sed -E 's/.*\[trigger deploy ([a-zA-Z].*\]).*/\1/' <<< $TRAVIS_COMMIT_MESSAGE)"

if [[ "$DEPLOY_FILES" == "$TRAVIS_COMMIT_MESSAGE" ]]; then
  echo "Not found any service to deploy"
else

  echo "Services to deploy : ${DEPLOY_FILES:0:-1}"

  #Build images according to changes
  for i in $(echo ${DEPLOY_FILES:0:-1} | tr ";" "\n")
  do
    if [[ "$BUILD_REACT_WEBAPP_JHIPSTER_IMAGE" == "false" ]] && [[ "$i" == "react-webapp-jhipster" ]]; then
      IMAGES_TO_BUILD+="react-webapp-jhipster;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_REACT_WEBAPP_JHIPSTER_IMAGE=true
    elif [[ "$BUILD_REACT_WEBAPP_IMAGE" == "false" ]] && [[ "$i" == "react-webapp" ]]; then
      IMAGES_TO_BUILD+="react-webapp;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_REACT_WEBAPP_IMAGE=true
    elif [[ "$BUILD_NODE_IMAGE" == "false" ]] && [[ "$i" == "nodejs-service"  ]]; then
      IMAGES_TO_BUILD+="week-menu-api;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_NODE_IMAGE=true
    elif [[ "$BUILD_AUTHENTICATION_SERVICE_IMAGE" == "false" ]] && [[ "$i" == "authentication-service" ]]; then
      mvn -B -f ./authentication-service/pom.xml docker:build
      IMAGES_TO_BUILD+="authentication-service;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_AUTHENTICATION_SERVICE_IMAGE=true
    elif [[ "$BUILD_USER_SERVICE_IMAGE" == "false" ]] && [[ "$i" == "user-service" ]]; then
      mvn -B -f ./user-service/pom.xml docker:build
      IMAGES_TO_BUILD+="user-service;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_USER_SERVICE_IMAGE=true
    elif [[ "$BUILD_PERSON_SERVICE_IMAGE" == "false" ]] && [[ "$i" == "person-service" ]]; then
      mvn -B -f ./person-service/pom.xml docker:build
      IMAGES_TO_BUILD+="person-service;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_PERSON_SERVICE_IMAGE=true
    elif [[ "$BUILD_KOTLIN_SERVICE_IMAGE" == "false" ]] && [[ "$i" == "kotlin-service" ]]; then
      mvn -B -f ./kotlin-service/pom.xml docker:build
      IMAGES_TO_BUILD+="kotlin-service;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_KOTLIN_SERVICE_IMAGE=true
    fi

  done

  echo "Should build new docker image for react webapp? ${BUILD_REACT_WEBAPP_IMAGE}"

  echo "Should build new docker image for react webapp jhipster? ${BUILD_REACT_WEBAPP_JHIPSTER_IMAGE}"

  echo "Should build new docker image for nodejs? ${BUILD_NODE_IMAGE}"

  echo "Should build new docker image for authentication-service? ${BUILD_AUTHENTICATION_SERVICE_IMAGE}"

  echo "Should build new docker image for user-service? ${BUILD_USER_SERVICE_IMAGE}"

  echo "Should build new docker image for person-service? ${BUILD_PERSON_SERVICE_IMAGE}"

  echo "Should build new docker image for kotlin-service? ${BUILD_KOTLIN_SERVICE_IMAGE}"

  echo "List of images to build: $IMAGES_TO_BUILD"

  if [[ "$BUILD_NEW_DOCKER_IMAGE" == "true" ]]; then
    # Install Google Sdk
    if [ ! -d "$HOME/google-cloud-sdk/bin" ]; then
      rm -rf $HOME/google-cloud-sdk; curl -s https://sdk.cloud.google.com | bash;
    fi

    source /home/travis/google-cloud-sdk/path.bash.inc

    gcloud --quiet version

    gcloud --quiet components update

    gcloud --quiet components update kubectl

    #Deploying docker images to Google Cloud
    echo "Images to build: $IMAGES_TO_BUILD"

    for DOCKER_IMAGE in $(echo $IMAGES_TO_BUILD | tr ";" "\n")
    do
      echo "Preparing to deploy docker image $DOCKER_IMAGE"
      if [ "$DOCKER_IMAGE" == "react-webapp-jhipster" ]; then
        echo "Building Docker React Web JHipster App Image..."
        npm --prefix ./react-webapp-jhipster install ./react-webapp-jhipster
        docker build --quiet --build-arg PORT=$REACT_WEBAPP_PORT -t eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT ./react-webapp-jhipster
      elif [ "$DOCKER_IMAGE" == "react-webapp" ]; then
        echo "Building Docker React Web App Image..."
        npm --prefix ./react-webapp install ./react-webapp
        docker build --quiet --build-arg PORT=$REACT_WEBAPP_PORT -t eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT ./react-webapp
      elif [ "$DOCKER_IMAGE" == "week-menu-api" ]; then
        echo "Building Docker NodeJS Service Image..."
        npm --prefix ./nodejs-service install ./nodejs-service
        docker build --quiet --build-arg SERVER_PORT=$NODE_SERVER_PORT -t eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT ./nodejs-service
      else
        echo "Tagging docker image $DOCKER_IMAGE..."
        docker tag ${DOCKER_IMAGE}:latest eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT
      fi

      echo $GCLOUD_SERVICE_KEY_PROD | base64 --decode -i > ${HOME}/gcloud-service-key.json
      gcloud auth activate-service-account --key-file ${HOME}/gcloud-service-key.json

      gcloud --quiet config set project $GCP_PROJECT_ID
      gcloud --quiet config set container/cluster $GCP_CLUSTER
      gcloud --quiet config set compute/zone $GCP_ZONE
      gcloud --quiet container clusters get-credentials $GCP_CLUSTER

      echo "Pushing docker image $DOCKER_IMAGE..."
      gcloud docker -- push eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}

      echo "Adding tag image to latest container image $DOCKER_IMAGE..."
      gcloud components install beta --quiet
      yes | gcloud beta container images add-tag eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:$TRAVIS_COMMIT eu.gcr.io/${GCP_PROJECT_ID}/${DOCKER_IMAGE}:latest

      kubectl config view
      kubectl config current-context

      echo "Deploying new docker image $DOCKER_IMAGE..."
      kubectl patch deployment ${DOCKER_IMAGE} -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"date\":\"`date +'%s'`\"}}}}}"
    done
  fi

fi
