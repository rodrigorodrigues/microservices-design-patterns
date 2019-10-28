#!/bin/sh

set -xeuo pipefail

if ! curl -k --retry 5 --retry-connrefused --retry-delay 0 -sf http://grafana:3000/api/dashboards/name/prom; then
    curl -sf -X POST -H "Content-Type: application/json" \
         --data-binary '{"name":"prom","type":"prometheus","url":"http://prometheus:9090","access":"proxy","isDefault":true}' \
         http://grafana:3000/api/datasources
fi

dashboard_id=4701
last_revision=$(curl -sf https://grafana.com/api/dashboards/${dashboard_id}/revisions | grep '"revision":' | sed 's/ *"revision": \([0-9]*\),/\1/' | sort -n | tail -1)

echo '{"dashboard": ' > data.json
curl -s https://grafana.com/api/dashboards/${dashboard_id}/revisions/${last_revision}/download >> data.json
echo ', "inputs": [{"name": "DS_PROMETHEUS", "pluginId": "prometheus", "type": "datasource", "value": "prom"}], "overwrite": false}' >> data.json
curl -k --retry-connrefused --retry 5 --retry-delay 0 -sf \
     -X POST -H "Content-Type: application/json" \
     --data-binary @data.json \
     http://grafana:3000/api/dashboards/import
