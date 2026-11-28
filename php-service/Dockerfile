FROM composer:2 AS vendor

WORKDIR /app
COPY composer.json composer.lock ./
RUN composer install --no-dev --no-scripts --no-autoloader --prefer-dist --ignore-platform-reqs --no-interaction

COPY . .
RUN composer dump-autoload --no-dev --optimize --classmap-authoritative --no-scripts


FROM dunglas/frankenphp:php8.4

ARG PORT=8088
ENV SERVER_PORT=${PORT} \
    APP_ENV=production \
    APP_DEBUG=false \
    LOG_CHANNEL=stderr

RUN install-php-extensions mongodb apcu opcache pcntl \
    && cp "$PHP_INI_DIR/php.ini-production" "$PHP_INI_DIR/php.ini"

WORKDIR /app
COPY --from=vendor /app /app
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

RUN rm -f bootstrap/cache/*.php \
    && php artisan package:discover --ansi \
    && chmod +x /usr/local/bin/docker-entrypoint.sh \
    && chown -R www-data:www-data storage bootstrap/cache /data/caddy /config/caddy

USER www-data

EXPOSE ${PORT}

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s \
    CMD curl -fs "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["docker-entrypoint.sh"]
