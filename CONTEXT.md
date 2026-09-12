# Authentication

How services in this system issue, publish, and verify JSON Web Tokens. `authentication-service` is the sole issuer; every other service (`scala-address-service`, `person-service`, `go-service`, etc.) is a verifier only.

## Language

**JWT**:
The bearer credential issued by `authentication-service` and sent by clients on every request to prove identity and carry authorities. Verifying services never mint their own.

**Claims**:
The statements encoded in a JWT's payload — subject, expiration, issuer, and the `authorities` claim.

**Authority**:
A single granted permission string carried in a JWT's `authorities` claim (e.g. `ROLE_ADMIN`, `ROLE_PERSON_READ`). This is the established system-wide term — `authentication-service` and `person-service` both configure `authoritiesClaimName("authorities")`, and `authentication-common` has a domain model class named `Authority`.
_Avoid_: Role, Permission, Scope.

**JWKS (JSON Web Key Set)**:
The set of verification keys `authentication-service` publishes at `/.well-known/jwks.json` so other services can check a JWT's signature without ever holding the private signing key. It serves an RSA key in the `prod` profile and an HMAC key wrapped as a JWK everywhere else (`JwksSymmetricController`) — the endpoint always returns a JWK regardless of algorithm.

**Issuer**:
`authentication-service`. The only service allowed to mint JWTs. All other services are verifiers.

**Verification key value** (`com.microservice.authentication.jwt.key-value`):
A symmetric secret or PEM-encoded public key handed to a verifying service directly, used instead of a live JWKS fetch — e.g. in tests or local/offline dev. Defined by `authentication-common`'s `AuthenticationProperties` and already used by `authentication-service` and `person-service`'s test config.
_Avoid_: `JWT_SECRET` — not the established property name.

**JWKS URI** (`com.microservice.authentication.jwk.key-set-uri`):
The property name a verifying service reads to locate the issuer's JWKS document.
_Avoid_: `JWKS_URL` — not the established property name.

**Unverified mode** (`com.microservice.authentication.jwt.allow-unverified`):
An explicit opt-in flag that lets a service skip JWT signature verification entirely. Off by default — see [ADR-0002](./docs/adr/0002-jwt-verification-fails-closed-by-default.md).
