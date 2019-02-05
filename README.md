## Spring Boot 2 + Spring Cloud + WebFlux + React SPA + Node.js + MongoDB

Example of using Microservices Architecture with multiple languages(`Java, NodeJS, Kotlin, Scala`).

The same approach could be applied for any language.

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

### Microservice Diagram
 
![Microservice Architecture](Microservice.png?raw=true "Microservice Architecture") 

### Prerequisites
 * JDK 1.8
 * Maven
 * Docker/Docker Compose

### Install

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

After that access the app by `http://localhost:3000`

Following list of default users for login:

```
admin@gmail.com/password

master@gmail.com/password123

anonymous@gmail.com/test
```

### Swagger UI

Swagger UI is available for `Authentication, Person and User Services`
[Swagger UI](http://localhost:{PORT}/swagger-ui.html)

### Run using Docker Compose

You can run everything using docker-compose on `docker folder` just run the following commands:

```
docker-compose up service-discovery config-management

In another console
docker-compose up
```

### TODO List

* [X] Java - Split Person and User in different entities
* [X] Java - Split back-end and front-end in two different folders
* [X] Java - Split Java 8 Learning in another folder
~~* [ ] Java - Add Cloud Foundry for deploy~~
* [ ] Java - Add Heroku for deploy
* [ ] Java - Add Test for Users Classes
* [X] Java - Add Spring Cloud Config
* [X] Java - Add Service Discovery(Eureka)
* [ ] Java - Add Zuul(Gateway)
* [X] Java - Add Maven Docker Plugin
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
