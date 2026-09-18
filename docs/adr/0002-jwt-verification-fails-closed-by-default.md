# JWT verification fails closed by default

Context: `scala-address-service`'s original `JwtDirectives` silently skipped signature verification — logging only a warning — whenever neither a JWKS URI nor a key value was configured, meaning any well-formed but unsigned/unverified token would be accepted. That's an insecure default: a missing environment variable in a real deployment would fail open rather than loudly.

Decision: if neither `com.microservice.authentication.jwk.key-set-uri` nor `com.microservice.authentication.jwt.key-value` is configured, the service now refuses to start (throws during startup, before the HTTP port binds) instead of serving unverifiable JWTs. Skipping verification is only reachable via an explicit `com.microservice.authentication.jwt.allow-unverified=true` opt-in, intended for local/offline dev. See [CONTEXT.md](../../CONTEXT.md) for the vocabulary.
