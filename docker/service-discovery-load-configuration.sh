#!/bin/sh

set -xeuo pipefail

if ! curl -k --retry 5 --retry-connrefused --retry-delay 5 -H "Content-Type: application/json" -sf http://service-discovery:8500/v1/kv/config; then
    curl --request PUT --data-binary @spring-cloud-config/applicationDev.yml http://service-discovery:8500/v1/kv/config/application,dev/data
    curl --request PUT --data-binary @spring-cloud-config/applicationProd.yml http://service-discovery:8500/v1/kv/config/application,prod/data
    curl --request PUT --data-binary @spring-cloud-config/application.yml http://service-discovery:8500/v1/kv/config/application/data
    curl --request PUT --data-binary @spring-cloud-config/edge-server.yml http://service-discovery:8500/v1/kv/config/edge-server/data
    curl --request PUT --data-binary @spring-cloud-config/authentication-service.yml http://service-discovery:8500/v1/kv/config/authentication-service/data
    curl --request PUT --data-binary @spring-cloud-config/quarkus-service.yml http://service-discovery:8500/v1/kv/config/quarkus-service/data
fi
