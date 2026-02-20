# Clojure Service

A Clojure microservice for product management with Consul service discovery.

## Prerequisites

- Java 11+
- Leiningen
- MongoDB
- Consul (optional)

## Running

```bash
lein run
```

## Building

```bash
lein uberjar
```

## Testing

```bash
lein test
```

## Environment Variables

- `SERVER_PORT` - Server port (default: 8087)
- `MONGODB_URI` - MongoDB connection URI (default: mongodb://localhost:27017/docker)
- `CONSUL_URL` - Consul URL (default: http://localhost:8500)
- `JWT_SECRET_KEY` - JWT secret key (default: my-safe-secret)
