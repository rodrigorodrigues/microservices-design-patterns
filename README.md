# Spring Boot 2 + Spring Cloud + WebFlux + React SPA + Node.js + MongoDB

Example of using `Microservices Architecture` with multiple languages(`Java, NodeJS, Kotlin`).

Inspired from [Microservices Patterns Book](https://www.manning.com/books/microservices-patterns).

The same approach could be applied for any language.

## Table of Contents

  1. [Introduction](#introduction)
  2. [Prerequisites](#prerequisites)
  3. [Microservice Diagram](#microservice-diagram)
  4. [Manual Installation](#manual-installation)
  5. [Using Docker and Docker Compose](#installation-using-docker-and-docker-compose)
  6. [Prometheus and Grafana with Docker Compose](#prometheus-and-grafana)
  7. [Zipkin Request Tracing with Docker Compose](#request-tracing-zipkin)
  7. [React App](#react-app)
  8. [Default Users](#default-users)
  9. [TODO-LIST](#todo-list)
  10. [References](#references)
  11. [Postman Collection](docs/postman_collection.json?raw=true)

Used `Spring Cloud Netflix` for Microservices patterns(`Service Discovery, Config Management and Monitoring`):

Used `Spring Boot 2 Webflux` for Reactive Programming.

Used `React` for Single Page Application.

Following description for each folder.

 * Service Discovery - `eureka-server`
 * Config Management - `config-server`
 * Gateway - `edge-server`
 * Monitoring - `admin-edger`
 
Exposed RestFul APIs:
  * authentication-service - `POST - /api/authenticate`
  * user-service - `CRUD - /api/users/**`
  * person-service - `CRUD - /api/persons/**`
  * nodejs-service - `CRUD - /v2/category`

React App:
 * react-webapp - React using Bootstrap

### Prerequisites
 * JDK 1.8
 * Maven 3
 * Docker 17.05.0-ce+
 * Docker Compose 1.23.2

### Microservice Diagram
 
![Microservice Architecture](Microservice.png?raw=true "Microservice Architecture") 

### Manual Installation

On `root folder`

`mvn clean install docker:build`

### Run Spring Boot

Run `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port={PORT}"` in the following `Microservices folders`

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

### Run Node.js service

On `nodejs-service folder` run the following commands:

```
sudo npm install

sudo npm start
```

### Run React Web app

On `react-webapp folder` run the following commands:

```
sudo npm install

sudo npm start
```

### Installation Using Docker and Docker Compose

You can run everything using docker-compose on `docker folder` just run the following command:

```
docker-compose up -d
```

PS: Whenever change is made it should rebuild the image using the following command:

``` 
docker-compose up --build week-menu-api react-webapp
```

To see a log inside a docker container:

```bash
docker logs -f SERVICE_NAME
```
PS: Services Names are in `docker-compose.yml -> key container_name`

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

### Prometheus and Grafana

`Prometheus` is a tool for monitoring applications and generating metrics.

`Grafana` is a tool for communicate with Prometheus and display the data in dashboards.

Spring Boot 2 by default uses [Micrometer](https://micrometer.io) for monitoring JVM/Microservices Applications.

To access prometheus `http://localhost:9090` and grafana `http://localhost:3000`.

### Request Tracing Zipkin

`Zipkin` is used for request tracing through the microservices.

To access it `http://localhost:9411`.

### React App

After that access the app by `http://localhost:3001`

### Default Users

Following list of default users:

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
* [ ] React - Fix Person List to work with `@Tailable` and `EventSource`.

### References
[Microservices Patterns](https://microservices.io)

[What's new in Java 8](https://leanpub.com/whatsnewinjava8/read)

[Spring Guide](https://spring.io/guides)

[Spring Boot](https://start.spring.io)

[React ad Spring WebFlux](https://developer.okta.com/blog/2018/09/25/spring-webflux-websockets-react)

[Spring WebFlux Security Jwt](https://github.com/raphaelDL/spring-webflux-security-jwt)

[Junit 5](https://medium.com/@GalletVictor/migration-from-junit-4-to-junit-5-d8fe38644abe)
