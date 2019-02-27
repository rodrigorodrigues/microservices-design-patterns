# Microservice Architecture with Multiple Languages

The main idea for this project is to apply `Microservice Architecture by Chris Richardson` using multiple languages.

Most all services are made in `Java with Spring Boot 2 + Webflux` but one is in `NodeJS` and frontend in `React`. 

Does not matter which language you peek this pattern could be applied for in any language.

Inspired from the book [Microservices Patterns](https://www.manning.com/books/microservices-patterns) by `Chris Richardson - @crichardson`.

## Contents

  1. [Microservice Patterns](#microservice-patterns)
  2. [Prerequisites](#prerequisites)
  3. [Microservice Diagram](#microservice-diagram)
  4. [Installing all services using Docker Compose](#installing-all-services-using-docker-compose)
  5. [Docker Commands](#docker-commands)
  6. [Monitoring - Spring Boot Admin](#monitoring---spring-boot-admin)
  6. [Service Discovery - Eureka](#service-discovery---eureka)
  6. [Externalized Configuration - Spring Config](#externalized-configuration---spring-cloud-config)
  6. [Prometheus and Grafana](#prometheus-and-grafana)
  7. [Zipkin Request Tracing](#request-tracing-zipkin)
  8. [Manual Installation - NOT RECOMMENDED](#manual-installation---not-recommended)
  9. [Accessing React Web App](#accessing-react-app)
  10. [List of default users](#default-users)
  11. [TODO-LIST](#todo-list)
  12. [References](#references)
  13. [Postman Collection](docs/postman_collection.json?raw=true)

### Microservice Patterns

 * **Server-side service discovery** - `eureka-server`
 
 * **Client-side service discovery** - All servers/services(`admin-server, user-service, person-service, etc`)
 
 * **API Gateway** - `edge-server`
 
 * **Externalized configuration** - `config-server`
 
 * **Log aggregation** - `admin-server`
 
 * **Access token** - [JSON Web Token](https://jwt.io) for `Authentication/Authorization` on services(`user-service, person-service, authentication-service, nodejs-service`)
 
 * **Health Check API** - All Java services are using [Spring Boot Actuator Starter](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready) - `<artifactId>spring-boot-starter-actuator</artifactId>` and NodeJS is using `express-actuator`
 
 * **Distributed tracing** - All Java services are using [Spring Cloud Sleuth](https://spring.io/projects/spring-cloud-sleuth) `<artifactId>spring-cloud-starter-sleuth</artifactId>`, Zipkin `<artifactId>spring-cloud-starter-zipkin</artifactId>` and NodeJS is using `morgan, zipkin, zipkin-transport-http and zipkin-context-cls`
 
 * **Application metrics** - All Java services are using [Spring Micrometer Prometheus](https://spring.io/blog/2018/03/16/micrometer-spring-boot-2-s-new-application-metrics-collector) - `<artifactId>micrometer-registry-prometheus</artifactId>` and NodeJS is using `express-prom-bundle`
 
 * **Database per service** - All services are using `MongoDB` as database

To know more about each pattern find at [Microservice Architecture](https://microservices.io/patterns/microservices.html)

### Prerequisites
 * JDK 1.8
 * Maven 3
 * Docker 17.05.0-ce+ - `Not necessary but recommended otherwise the services should run by command`
 * Docker Compose 1.23.2 - `Not necessary but recommended otherwise the services should run by command`

### Microservice Diagram
 
![Microservice Architecture](docs/Microservice.png "Microservice Architecture") 

### Manual Installation - NOT RECOMMENDED

If for some reason you cannot install `docker/docker-compose` you can run all services manually.

On `root folder` run the following command at once:

`mvn clean install docker:build`

**Run Spring Boot**

To run the services use the following command in each `Microservices folders`:

`mvn spring-boot:run -Dspring-boot.run.arguments="--server.port={PORT}"`

```
eureka-server - PORT=8761
config-server - PORT=8888
edge-server - PORT=9006
admin-server - PORT=9000
authentication-service - PORT=8081
person-service - PORT=8082
user-service - PORT=8083
```

PS: To login at `Eureka/Config/Edge/Admin` need a user with role `ADMIN`. See at [Default Users](#default-users)

**Run Node.js service**

On `nodejs-service folder` run the following commands:

```
sudo npm install

sudo npm start
```

**Run React Web app**

On `react-webapp folder` run the following commands:

```
sudo npm install

sudo npm start
```

### Installing All Services using Docker Compose

You can run everything using docker-compose on `docker folder` just run the following command:

```
docker-compose up -d
```

PS: Whenever change is made it should rebuild the image using the following command:

``` 
docker-compose up --build week-menu-api react-webapp
```

### Docker Commands

To see a log inside a docker container:

```bash
docker logs -f SERVICE_NAME
```
PS: Services Names are name of `container_name`

To execute a command inside the container:

```bash
docker exec -it week-menu-api sh
```

To stop and remove all containers:
```bash
docker-compose down -v
```

To restart/start/stop/remove specific container:

```bash
docker-compose restart SERVICE_NAME
docker-compose up SERVICE_NAME
docker-compose stop SERVICE_NAME
docker-compose rm SERVICE_NAME
```

### Monitoring - Spring Boot Admin

To see information related to `registered microservices` use [Spring Boot Admin](http://localhost:9000).

![Spring Boot](docs/spring_boot_admin.png)

PS: Need to login with a valid user and role `ADMIN`. See at [Default Users](#default-users)

### Service Discovery - Eureka

To see `all registered microservices` to use [Eureka](http://localhost:8761).

![Eureka](docs/eureka.png)

PS: Need to login with a valid user and role `ADMIN`. See at [Default Users](#default-users)

### Externalized Configuration - Spring Cloud Config

To see configuration related to specific service use [Spring Config](http://localhost:8888/edge-server/default).

![Spring Config](docs/spring_config.png)

PS: Need to login with a valid user and role `ADMIN`. See at [Default Users](#default-users)

### Prometheus and Grafana

`Prometheus` is a tool for generating metrics from the requests.

`Grafana` is a tool for communicate with `Prometheus` and display the data in dashboards.

Spring Boot 2 by default uses [Micrometer](https://micrometer.io) for monitoring `JVM/Microservices Applications`.

To access [Prometheus UI](http://localhost:9090)

![Prometheus](docs/prometheus.png) 
 
To access [Grafana Dashboard](http://localhost:3000).

![Grafana](docs/grafana.png)

PS: It depends on docker.

### Sleuth and Zipkin

`Sleuth` is used for creating a unique identifier(Span) and set to a request for all `microservices calls`.

`Zipkin` is used for request tracing through `microservices`.

To access [Zipkin UI](http://localhost:9411).

![Zipkin1](docs/zipkin1.png)
![Zipkin2](docs/zipkin2.png)

### Accessing React App

To access [React Web App](http://localhost:3001).

![React1](docs/react1.png)
![React2](docs/react2.png)

### Default Users

Following the list of default users:

```
admin@gmail.com/password - ROLE_ADMIN

master@gmail.com/password123 - ROLE_PERSON_CREATE, ROLE_PERSON_READ, ROLE_PERSON_SAVE

anonymous@gmail.com/test - ROLE_PERSON_READ
```

### Swagger UI

Swagger UI is available for `Authentication, Person and User Services`
[Swagger UI](http://localhost:{PORT}/swagger-ui.html)

### TODO List

* [X] Java - Split Person and User in different entities
* [X] Java - Split back-end and front-end in two different folders
* [X] Java - Split Java 8 Learning in another folder
* [ ] ~~Java - Add Cloud Foundry for deploy~~
* [ ] Java - Add Heroku for deploy
* [X] Java - Add Test for Users Classes
* [X] Java - Add Spring Cloud Config
* [X] Java - Add Service Discovery(Eureka)
* [X] Java - Add Zuul(Gateway)
* [X] Java - Add Maven Docker Plugin
* [X] Java - Add Redis for Shared Session between applications
* [X] Java - Add Authentication for all applications
* [X] Java - Add Prometheus/Grafana for docker compose
* [X] Java - Fix Zuul/Edge Server for working with NodeJS Service
* [ ] Kotlin - Add Service using Kotlin Language
* [ ] Scala - Add Service using Scala Language
* [ ] C# - Add Service using C# Language
* [X] React - Create User List
* [ ] React - Create User Page
* [ ] React - Create User Edit
* [ ] React - Fix User Create/Edit
* [ ] React - Fix Person Create/Edit
* [ ] React - Fix Person List to work with `@Tailable` and `EventSource`.

### References
[Pattern Microservice Architecture](https://microservices.io/patterns/microservices.html)

[Spring Guide](https://spring.io/guides)

[Spring Boot](https://start.spring.io)

[React ad Spring WebFlux](https://developer.okta.com/blog/2018/09/25/spring-webflux-websockets-react)

[Spring WebFlux Security Jwt](https://github.com/raphaelDL/spring-webflux-security-jwt)

[Junit 5](https://medium.com/@GalletVictor/migration-from-junit-4-to-junit-5-d8fe38644abe)
