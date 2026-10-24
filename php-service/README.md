# PHP Service (Books API)

Manages books. Written in PHP with [Laravel](https://laravel.com/) and served by [FrankenPHP](https://frankenphp.dev/).
Shared concepts (Consul, JWT, roles) are covered in [Services How-To](../docs/services-how-to.md).

| | |
|---|---|
| Docker Compose name | `book-api`, port 8088 |
| Gateway route | `/api/books/**` |
| PHP version | 8.4 (Laravel 13) |

## Stack

- HTTP: Laravel 13 routing and validation, FrankenPHP in the Docker image
- Database: `mongodb/laravel-mongodb` (needs the `mongodb` PHP extension)
- Auth: `firebase/php-jwt` for HS256/RS256 tokens and JWKS
- Discovery and config: Consul HTTP API through Laravel's HTTP client, `symfony/yaml`
- Metrics: `promphp/prometheus_client_php`, kept in APCu between requests
- API docs: OpenAPI file in `public/api-docs/openapi.yaml`, Swagger UI from a CDN

## Run with Docker Compose

```bash
docker compose -f docker/docker-compose.yml up -d book-api
```

## Run locally

Needs PHP 8.3+ with the `mongodb` extension (`pecl install mongodb`) and a MongoDB. Consul is optional (`CONSUL_ENABLED=false` by default).

```bash
cd php-service
composer install
cp .env.example .env            # then set one JWT option, see below
php artisan service:prepare     # checks JWT settings, creates indexes, registers in Consul if enabled
php artisan serve --port=8088
```

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/docker` | MongoDB connection; the database comes from the URI path |
| `MONGODB_DATABASE` | | Overrides the database from the URI |
| `SERVER_PORT` | `8088` | HTTP port (Docker image) and port registered in Consul |
| `APP_NAME` | `php-service` | Name registered in Consul; the gateway routes to `lb://php-service` |
| `HOST_NAME` | `localhost` | Address registered in Consul |
| `COM_MICROSERVICE_AUTHENTICATION_JWK_KEY_SET_URI` | | JWKS URI; preferred when set |
| `COM_MICROSERVICE_AUTHENTICATION_JWT_KEY_VALUE` | | HS256 secret, or a PEM public key for RS256 |
| `COM_MICROSERVICE_AUTHENTICATION_JWT_ALLOW_UNVERIFIED` | `false` | Skips signature checks. Local development only |
| `JWT_AUTHORITIES_CLAIM` | `authorities` | Claim holding the authorities |
| `JWKS_CACHE_TTL` | `300` | Seconds a fetched JWKS is cached |
| `CONSUL_ENABLED` | `false` | Register in Consul and read config from it |
| `CONSUL_URL` | `http://localhost:8500` | Consul agent; `service-discovery:8500` without a scheme also works |
| `SPRING_PROFILES_ACTIVE` | `dev` | The first profile picks the Consul key `config/application,<profile>/data` |
| `CONSUL_CONFIG_CACHE_TTL` | `300` | Seconds the Consul config is cached |
| `LOG_LEVEL` | `debug` | Log level; logs go to stderr |

**JWT verification** follows [ADR-0001](../docs/adr/0001-hybrid-jwks-and-key-value-jwt-verification.md) and [ADR-0002](../docs/adr/0002-jwt-verification-fails-closed-by-default.md), like `scala-address-service`:

1. With a JWKS URI, keys are fetched from it and looked up by the token's `kid`. A token without a `kid` is accepted only if the key set has exactly one key. An unknown `kid` refetches the key set once, so key rotation works.
2. Otherwise, the key value is used: an HS256 secret, or RS256 if it is a PEM public key.
3. When neither is set by environment variables and Consul is enabled, both are read from Consul (`com.microservice.authentication.jwk.keySetUri` and `com.microservice.authentication.jwt.keyValue`, camelCase or kebab-case).
4. With none of them, `service:prepare` exits with an error and the container stops. Unverified mode must be turned on explicitly.

Tokens must have an `exp` claim. The token's algorithm must match the key's, so an HS256 token signed with an RSA public key is rejected.

## API

| Method | Path | Authorities |
|---|---|---|
| GET | `/api/books?page=0&size=10` | `ROLE_ADMIN`, `ROLE_BOOKS_READ`, `_CREATE`, `_SAVE`, `_DELETE` or `SCOPE_openid` |
| GET | `/api/books/{id}` | `ROLE_ADMIN`, `ROLE_BOOKS_READ`, `ROLE_BOOKS_SAVE` or `SCOPE_openid` |
| POST | `/api/books` | `ROLE_ADMIN`, `ROLE_BOOKS_CREATE` or `SCOPE_openid` |
| PUT | `/api/books/{id}` | `ROLE_ADMIN`, `ROLE_BOOKS_SAVE` or `SCOPE_openid` |
| DELETE | `/api/books/{id}` | `ROLE_ADMIN`, `ROLE_BOOKS_DELETE` or `SCOPE_openid` |

Request body: `{"title", "author", "isbn", "publishedYear", "price", "currency"}`, all required. `isbn` is unique (409 if taken), `currency` has 3 letters, `size` is at most 100. Validation errors return 422 with an `errors` object. Responses add `id`, `createdByUser`, `createdDate`, `lastModifiedByUser` and `lastModifiedDate`. The users come from the token's `sub`. Lists use the Spring page format (`content`, `number`, `size`, `totalPages`, `totalElements`, `first`, `last`).

```bash
curl -X POST http://localhost:8088/api/books \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"978-0132350884","publishedYear":2008,"price":37.5,"currency":"USD"}'
```

Also available without a token: `/actuator`, `/actuator/health` (includes MongoDB; 503 when down), `/actuator/info`, `/actuator/prometheus` (`http_server_requests_seconds` by method, route and status), Swagger UI at `/swagger-ui` and the spec at `/api-docs/openapi.yaml`.

## Tests

```bash
cd php-service
composer install --ignore-platform-req=ext-mongodb   # the flag is only needed without the extension
composer test
```

Unit and feature tests use an in-memory repository and fake HTTP responses, so they need neither MongoDB nor the `mongodb` extension. They cover JWT verification (every mode, algorithm confusion, key rotation, fail-closed), roles, the books API, actuator endpoints, Consul and the startup command.

The `Integration` suite runs `MongoBookRepository` against a real MongoDB. It is skipped unless the extension is loaded and `MONGODB_TEST_URI` is set:

```bash
docker run -d --rm --name books-mongo -p 27017:27017 mongo:7
MONGODB_TEST_URI=mongodb://localhost:27017/books_test vendor/bin/phpunit --testsuite Integration
```

CI runs all suites with MongoDB on pull requests that touch `php-service` ([workflow](../.github/workflows/docker-build-push-image-php-service.yml)). A master commit whose message contains `build php-service` also runs them and then pushes the image to Docker Hub.

## Known issues

- Without the APCu extension (e.g. `php artisan serve`), Prometheus counters only cover the current request. The Docker image has APCu.
- Swagger UI loads its assets from unpkg.com, so it needs internet access.
- When the JWT settings come from Consul and Consul is down as the cached copy expires (`CONSUL_CONFIG_CACHE_TTL`), requests fail with 500 until Consul is back. Setting the key value or JWKS URI as an environment variable avoids this.
