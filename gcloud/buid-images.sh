#!/bin/bash

set -e

TRAVIS_COMMIT=$1
TRAVIS_PULL_REQUEST=$2
TRAVIS_BRANCH=$3
echo "Travis CI Env: $TRAVIS_COMMIT"
echo "Travis Pull Request: $TRAVIS_PULL_REQUEST"
echo "Travis Branch: $TRAVIS_BRANCH"
export COMMITTER_EMAIL="$(git log -1 $TRAVIS_COMMIT --pretty="%cE")"
export AUTHOR_NAME="$(git log -1 $TRAVIS_COMMIT --pretty="%aN")"
export COMMIT_ID="$(git log -1 $TRAVIS_COMMIT --pretty="%H")"

BUILD_NEW_DOCKER_IMAGE=false
BUILD_REACT_WEBAPP_IMAGE=false
BUILD_NODE_IMAGE=false
BUILD_AUTHENTICATION_SERVICE_IMAGE=false
BUILD_USER_SERVICE_IMAGE=false
BUILD_PERSON_SERVICE_IMAGE=false
BUILD_KOTLIN_SERVICE_IMAGE=false
DIFF_FILES="$(git diff $TRAVIS_COMMIT --name-only)"

echo "Continuous Integration/Deployment for Commit ID($COMMIT_ID) from Author($AUTHOR_NAME - $COMMITTER_EMAIL)";

echo "Git Diff Files: $DIFF_FILES"

for i in $(echo $DIFF_FILES | tr " " "\n")
do
  if [[ "$BUILD_REACT_WEBAPP_IMAGE" == "false" ]] && [[ "$i" == *"react-webapp/"* ]] && [[ "$i" != *"/test/"* ]]; then
    if [[ "$i" =~ \.(css|js|html)$ ]]; then
      IMAGES_TO_BUILD+="react-webapp;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_REACT_WEBAPP_IMAGE=true
    fi
  elif [[ "$BUILD_NODE_IMAGE" == "false" ]] && [[ "$i" == *"nodejs-service/"*  ]] && [[ "$i" != *"/test/"* ]]; then
    if [[ "$i" =~ \.(js)$ ]]; then
      IMAGES_TO_BUILD+="week-menu-api"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_NODE_IMAGE=true
    fi
  elif [[ "$BUILD_AUTHENTICATION_SERVICE_IMAGE" == "false" ]] && [[ "$i" == "authentication-service/"* ]] && [[ "$i" != *"/test/"* ]]; then
    if [[ "$i" =~ \.(java|yml|xml)$ ]]; then
      mvn -B -f ../authentication-service/pom.xml docker:build
      IMAGES_TO_BUILD+="authentication-service;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_AUTHENTICATION_SERVICE_IMAGE=true
    fi
  elif [[ "$BUILD_USER_SERVICE_IMAGE" == "false" ]] && [[ "$i" == "user-service/"* ]] && [[ "$i" != *"/test/"* ]]; then
    if [[ "$i" =~ \.(java|yml|xml)$ ]]; then
      mvn -B -f ../user-service/pom.xml docker:build
      IMAGES_TO_BUILD+="user-service;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_USER_SERVICE_IMAGE=true
    fi
  elif [[ "$BUILD_PERSON_SERVICE_IMAGE" == "false" ]] && [[ "$i" == "person-service/"* ]] && [[ "$i" != *"/test/"* ]]; then
    if [[ "$i" =~ \.(java|yml|xml)$ ]]; then
      mvn -B -f ../person-service/pom.xml docker:build
      IMAGES_TO_BUILD+="person-service;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_PERSON_SERVICE_IMAGE=true
    fi
  elif [[ "$BUILD_KOTLIN_SERVICE_IMAGE" == "false" ]] && [[ "$i" == "kotlin-service/"* ]] && [[ "$i" != *"/test/"* ]]; then
    if [[ "$i" =~ \.(java|yml|kt|xml)$ ]]; then
      mvn -B -f ../kotlin-service/pom.xml docker:build
      IMAGES_TO_BUILD+="kotlin-service;"
      BUILD_NEW_DOCKER_IMAGE=true
      BUILD_KOTLIN_SERVICE_IMAGE=true
    fi
  fi

done

echo "Should build new docker image for react webapp? ${BUILD_REACT_WEBAPP_IMAGE}"

echo "Should build new docker image for nodejs? ${BUILD_NODE_IMAGE}"

echo "Should build new docker image for authentication-service? ${BUILD_AUTHENTICATION_SERVICE_IMAGE}"

echo "Should build new docker image for user-service? ${BUILD_USER_SERVICE_IMAGE}"

echo "Should build new docker image for person-service? ${BUILD_PERSON_SERVICE_IMAGE}"

echo "Should build new docker image for kotlin-service? ${BUILD_KOTLIN_SERVICE_IMAGE}"

echo "List of images to build: $IMAGES_TO_BUILD"

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]] && [[ "$BUILD_NEW_DOCKER_IMAGE" == "true" ]] && [[ "$TRAVIS_BRANCH" == "master" ]]; then
  ./install-google-cloud-sdk.sh && ./deploy-prod.sh;
else
  exit 1;
fi