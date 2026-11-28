#!/bin/sh
set -e

# Fails (and stops the container) when JWT verification is not configured,
# MongoDB is unreachable or Consul registration fails.
php artisan service:prepare

frankenphp php-server --root public --listen ":${SERVER_PORT:-8088}" &
server_pid=$!

shutdown() {
    php artisan service:deregister || true
    kill -TERM "$server_pid" 2>/dev/null || true
    wait "$server_pid"
    exit 0
}
trap shutdown TERM INT

wait "$server_pid"
