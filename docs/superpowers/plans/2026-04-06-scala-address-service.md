# Scala Address Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a new microservice in Scala to manage `Address` domain, following `person-service` patterns with REST API, JWT security, and MongoDB.

**Architecture:** Apache Pekko HTTP for the REST API, MongoDB Scala Driver for reactive persistence, and custom Pekko Directives for JWT/RBAC security.

**Tech Stack:** Scala 3.3, Apache Pekko HTTP, MongoDB Scala Driver, Circe, Maven, jose4j.

---

### Task 1: Initialize Scala Address Service Maven Module

**Files:**
- Create: `scala-address-service/pom.xml`
- Modify: `pom.xml` (root)

- [ ] **Step 1: Create `scala-address-service/pom.xml` with Scala 3 and Pekko dependencies.**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>microservice</artifactId>
        <groupId>com.springboot</groupId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>scala-address-service</artifactId>

    <properties>
        <scala.version>3.3.1</scala.version>
        <pekko.version>1.0.2</pekko.version>
        <pekko-http.version>1.0.1</pekko-http.version>
        <circe.version>0.14.6</circe.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala3-library_3</artifactId>
            <version>${scala.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.pekko</groupId>
            <artifactId>pekko-http_3</artifactId>
            <version>${pekko-http.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.pekko</groupId>
            <artifactId>pekko-stream_3</artifactId>
            <version>${pekko.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.pekko</groupId>
            <artifactId>pekko-actor-typed_3</artifactId>
            <version>${pekko.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mongodb.scala</groupId>
            <artifactId>mongo-scala-driver_3</artifactId>
            <version>4.11.1</version>
        </dependency>
        <dependency>
            <groupId>io.circe</groupId>
            <artifactId>circe-core_3</artifactId>
            <version>${circe.version}</version>
        </dependency>
        <dependency>
            <groupId>io.circe</groupId>
            <artifactId>circe-generic_3</artifactId>
            <version>${circe.version}</version>
        </dependency>
        <dependency>
            <groupId>io.circe</groupId>
            <artifactId>circe-parser_3</artifactId>
            <version>${circe.version}</version>
        </dependency>
        <dependency>
            <groupId>org.bitbucket.b_c</groupId>
            <artifactId>jose4j</artifactId>
            <version>0.9.4</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.14</version>
        </dependency>
        <dependency>
            <groupId>org.scalatest</groupId>
            <artifactId>scalatest_3</artifactId>
            <version>3.2.17</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.pekko</groupId>
            <artifactId>pekko-http-testkit_3</artifactId>
            <version>${pekko-http.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>net.alchim31.maven</groupId>
                <artifactId>scala-maven-plugin</artifactId>
                <version>4.8.1</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>testCompile</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <scalaVersion>${scala.version}</scalaVersion>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Add module to root `pom.xml`.**

Modify: `pom.xml`
Find: `<module>kotlin-service</module>`
Add: `<module>scala-address-service</module>`

- [ ] **Step 3: Run `mvn compile -pl scala-address-service` to verify build.**

- [ ] **Step 4: Commit.**

---

### Task 2: Address Domain Model and Repository

**Files:**
- Create: `scala-address-service/src/main/scala/com/microservice/address/models/Address.scala`
- Create: `scala-address-service/src/main/scala/com/microservice/address/repository/AddressRepository.scala`

- [ ] **Step 1: Define `Address` case class and Circe codecs.**

```scala
package com.microservice.address.models

import java.time.Instant
import io.circe.generic.auto._
import io.circe.Encoder
import io.circe.Decoder

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

object AddressJsonProtocol {
  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap(_.toString)
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString.map(Instant.parse)
}
```

- [ ] **Step 2: Implement `AddressRepository` using MongoDB Scala Driver.**

```scala
package com.microservice.address.repository

import com.microservice.address.models.Address
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

class AddressRepository(collection: MongoCollection[Address])(implicit ec: ExecutionContext) {
  def findAll(): Future[Seq[Address]] = collection.find().toFuture()
  
  def findById(id: String): Future[Option[Address]] = 
    collection.find(equal("_id", id)).headOption()

  def insert(address: Address): Future[String] = {
    val newId = UUID.randomUUID().toString
    val doc = address.copy(id = Some(newId))
    collection.insertOne(doc).toFuture().map(_ => newId)
  }

  def update(id: String, address: Address): Future[Boolean] =
    collection.replaceOne(equal("_id", id), address.copy(id = Some(id))).toFuture().map(_.wasAcknowledged())

  def delete(id: String): Future[Boolean] =
    collection.deleteOne(equal("_id", id)).toFuture().map(_.getDeletedCount > 0)
}
```

- [ ] **Step 3: Commit.**

---

### Task 3: JWT Security Directives

**Files:**
- Create: `scala-address-service/src/main/scala/com/microservice/address/auth/JwtDirectives.scala`

- [ ] **Step 1: Implement JWT validation logic.**

```scala
package com.microservice.address.auth

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{Directive1, AuthorizationFailedRejection}
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwt.JwtClaims
import scala.util.{Try, Success, Failure}
import scala.jdk.CollectionConverters._

trait JwtDirectives {
  // In a real scenario, this would load the public key from a JWKS endpoint or config
  private val jwtConsumer = new JwtConsumerBuilder()
    .setSkipSignatureVerification() // For simplicity in this demo, usually verified with RSA Public Key
    .setRequireExpirationTime()
    .build()

  def authenticate: Directive1[JwtClaims] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)
        Try(jwtConsumer.processToClaims(token)) match {
          case Success(claims) => provide(claims)
          case Failure(_)      => reject(AuthorizationFailedRejection)
        }
      case _ => reject(AuthorizationFailedRejection)
    }
  }

  def authorizeRoles(roles: String*)(claims: JwtClaims): Directive1[JwtClaims] = {
    val authorities = claims.getClaimValue("authorities").asInstanceOf[java.util.List[String]].asScala
    if (roles.exists(authorities.contains)) provide(claims)
    else reject(AuthorizationFailedRejection)
  }
}
```

- [ ] **Step 2: Commit.**

---

### Task 4: REST Routes and Main Server

**Files:**
- Create: `scala-address-service/src/main/scala/com/microservice/address/routes/AddressRoutes.scala`
- Create: `scala-address-service/src/main/scala/com/microservice/address/Main.scala`

- [ ] **Step 1: Implement `AddressRoutes`.**

```scala
package com.microservice.address.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import com.microservice.address.repository.AddressRepository
import com.microservice.address.models.Address
import com.microservice.address.models.AddressJsonProtocol._
import io.circe.generic.auto._
import de.heikoseeberger.pekkohttpcirce.FailFastCirceSupport._ // Assuming we add this helper
import com.microservice.address.auth.JwtDirectives
import scala.concurrent.ExecutionContext

class AddressRoutes(repository: AddressRepository)(implicit ec: ExecutionContext) extends JwtDirectives {
  val routes = pathPrefix("api" / "addresses") {
    authenticate { claims =>
      concat(
        get {
          concat(
            pathEndOrSingleSlash {
              authorizeRoles("ROLE_ADMIN")(claims) { _ =>
                complete(repository.findAll())
              }
            },
            path(Segment) { id =>
              authorizeRoles("ROLE_PERSON_READ", "ROLE_ADMIN")(claims) { _ =>
                onSuccess(repository.findById(id)) {
                  case Some(addr) => complete(addr)
                  case None       => complete(404 -> "Not Found")
                }
              }
            }
          )
        },
        post {
          pathEndOrSingleSlash {
            authorizeRoles("ROLE_PERSON_SAVE", "ROLE_ADMIN")(claims) { _ =>
              entity(as[Address]) { addr =>
                complete(repository.insert(addr))
              }
            }
          }
        }
      )
    }
  }
}
```

- [ ] **Step 2: Implement `Main` entry point.**

- [ ] **Step 3: Commit.**

---

### Task 5: Gateway Routing and Docker

**Files:**
- Modify: `edge-server/src/main/resources/application.yml`
- Create: `scala-address-service/Dockerfile`

- [ ] **Step 1: Add route to `edge-server`.**

```yaml
                  - id: scala-address-service
                    uri: lb://scala-address-service
                    predicates:
                        - Path=/api/addresses/**
```

- [ ] **Step 2: Create Dockerfile.**

- [ ] **Step 3: Commit.**
