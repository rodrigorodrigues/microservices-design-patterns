# Scala Address Service Design Spec

Create a new microservice using Scala to manage a new domain called `Address`. This service follows the `person-service` pattern with a REST API, JWT-based security, and MongoDB integration.

## 1. Architecture & Tech Stack

- **Language**: Scala 3.3 (LTS)
- **Web Framework**: Apache Pekko HTTP (open-source fork of Akka HTTP)
- **Persistence**: MongoDB Scala Driver (Reactive)
- **JSON Library**: Circe (type-safe encoding/decoding)
- **Build Tool**: Maven with `scala-maven-plugin`
- **Testing**: ScalaTest, Pekko HTTP TestKit, TestContainers

## 2. Domain Model: `Address`

The `Address` entity will be stored in a MongoDB collection named `addresses`.

```scala
case class Address(
  id: Option[String] = None,
  street: String,
  city: String,
  state: String,
  zipCode: String,
  country: String,
  createdByUser: Option[String] = None,
  createdDate: Option[Instant] = Some(Instant.now()),
  lastModifiedByUser: Option[String] = None,
  lastModifiedDate: Option[Instant] = Some(Instant.now())
)
```

## 3. REST API Endpoints

| Method | Path | Description | Required Role |
|---|---|---|---|
| `GET` | `/api/addresses` | List all addresses | `ROLE_ADMIN` |
| `GET` | `/api/addresses/{id}` | Get address by ID | `ROLE_PERSON_READ` |
| `POST` | `/api/addresses` | Create new address | `ROLE_PERSON_SAVE` |
| `PUT` | `/api/addresses/{id}` | Update address | `ROLE_PERSON_SAVE` |
| `DELETE` | `/api/addresses/{id}` | Delete address | `ROLE_ADMIN` |

## 4. Security (JWT & RBAC)

Since this is a Pekko-based service, security is implemented as Pekko HTTP Directives:

1. **Authentication**: Extract `Bearer` token from `Authorization` header.
2. **Verification**: Use `jose4j` to verify the JWT signature using the shared public key.
3. **Authorization**: Custom directive `authorizeRoles(roles: String*)` checks the `authorities` claim in the JWT.

## 5. Infrastructure Integration

- **Consul**: The service registers itself with Consul on startup and provides a `/health` endpoint.
- **Docker**: Multi-stage Dockerfile optimized for Scala/Pekko.
- **Docker Compose**: Service entry in root `docker-compose.yml`.

## 6. Project Structure

```text
scala-address-service/
├── src/main/scala/com/microservice/address/
│   ├── Main.scala           (Entry point & Server setup)
│   ├── routes/              (Pekko HTTP Route definitions)
│   ├── models/              (Case classes & JSON formats)
│   ├── repository/          (MongoDB Scala Driver logic)
│   └── auth/                (JWT & Security Directives)
├── src/main/resources/
│   └── application.conf     (Pekko/Mongo settings)
└── pom.xml                  (Maven build with Scala support)
```
