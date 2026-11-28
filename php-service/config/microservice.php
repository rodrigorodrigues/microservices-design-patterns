<?php

/*
|--------------------------------------------------------------------------
| Microservice settings
|--------------------------------------------------------------------------
|
| JWT settings use the same property names as every other service, written
| as environment variables (see CONTEXT.md and docs/adr/0001):
|   com.microservice.authentication.jwk.key-set-uri
|   com.microservice.authentication.jwt.key-value
|   com.microservice.authentication.jwt.allow-unverified
|
*/

return [

    'authentication' => [
        // JWKS URI: preferred when set.
        'jwk_key_set_uri' => env('COM_MICROSERVICE_AUTHENTICATION_JWK_KEY_SET_URI'),

        // Verification key value: an HS256 secret or a PEM public key (RS256).
        // When empty and Consul is enabled, it is read from Consul.
        'jwt_key_value' => env('COM_MICROSERVICE_AUTHENTICATION_JWT_KEY_VALUE'),

        // Unverified mode: skip signature checks. Local development only (ADR-0002).
        'jwt_allow_unverified' => (bool) env('COM_MICROSERVICE_AUTHENTICATION_JWT_ALLOW_UNVERIFIED', false),

        // Claim holding the authorities list.
        'authorities_claim' => env('JWT_AUTHORITIES_CLAIM', 'authorities'),

        // Seconds a fetched JWKS document is cached for.
        'jwks_cache_ttl' => (int) env('JWKS_CACHE_TTL', 300),

        'leeway' => (int) env('JWT_LEEWAY', 30),
    ],

    'consul' => [
        'enabled' => (bool) env('CONSUL_ENABLED', false),
        'url' => env('CONSUL_URL', 'http://localhost:8500'),

        // Profile used for the shared Consul key config/application,<profile>/data.
        'profile' => explode(',', (string) env('SPRING_PROFILES_ACTIVE', 'dev'))[0],

        // Seconds the Consul configuration is cached for.
        'config_cache_ttl' => (int) env('CONSUL_CONFIG_CACHE_TTL', 300),
    ],

    'service' => [
        'name' => env('APP_NAME', 'php-service'),
        'host' => env('HOST_NAME', 'localhost'),
        'port' => (int) env('SERVER_PORT', 8088),
        'version' => env('APP_VERSION', '1.0.0'),
    ],

];
