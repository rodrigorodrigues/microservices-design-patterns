#!/bin/bash

set -e

if [ ! -d "$HOME/google-cloud-sdk/bin" ]; then
    rm -rf $HOME/google-cloud-sdk; curl https://sdk.cloud.google.com | bash;
fi

source /home/travis/google-cloud-sdk/path.bash.inc

gcloud --quiet version

gcloud --quiet components update

gcloud --quiet components update kubectl