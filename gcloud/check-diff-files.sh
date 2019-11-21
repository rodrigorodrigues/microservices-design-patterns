#!/bin/bash

set -e

BUILD_NEW_DOCKER_IMAGE=false
declare -a IMAGES_TO_BUILD=()

for i in $(echo $DIFF_FILES | tr " " "\n")
do

  if [[ "$i" == *"react-webapp/"* ]] ; then
    if [[ "$i" =~ \.(css|js|html)$ ]]; then
      IMAGES_TO_BUILD+="react-webapp"
      BUILD_NEW_DOCKER_IMAGE=true
    fi
  elif [[ "$i" == *"nodejs-service/"*  ]]; then
    if [[ "$i" =~ \.(js)$ ]]; then
      IMAGES_TO_BUILD+="nodejs-service"
      BUILD_NEW_DOCKER_IMAGE=true
    fi
  else
    if [[ "$i" == authentication-service* ]] && [[ "$i" =~ \.(java|yml)$ ]]; then
      IMAGES_TO_BUILD+=$i
      BUILD_NEW_DOCKER_IMAGE=true
    fi
  fi

done

echo "Should build new docker image? $BUILD_NEW_DOCKER_IMAGE"

echo "List of images to build: $IMAGES_TO_BUILD"

export BUILD_NEW_DOCKER_IMAGE="$BUILD_NEW_DOCKER_IMAGE";
export IMAGES_TO_BUILD="$IMAGES_TO_BUILD"