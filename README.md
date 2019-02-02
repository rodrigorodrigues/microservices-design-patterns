## Spring Boot 2 + Spring Cloud + WebFlux + React SPA + Node.js

### Prerequisites
 * JDK 1.8
 * Maven

### Install

On `root` folder

`mvn clean package docker:build`

### Run Spring Boot

On `microservices folders` run `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port={PORT}"`.

### Run React Web app

On `react-webapp` folder and access by http://localhost:3000

```
sudo npm install

sudo npm start
```

Following list of default users:

```
admin@gmail.com/password

master@gmail.com/password123

anonymous@gmail.com/test
```

### Swagger UI for Person and User Services
[Swagger UI](http://localhost:{PORT}/swagger-ui.html)

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
[What's new in Java 8](https://leanpub.com/whatsnewinjava8/read)

[Spring Guide](https://spring.io/guides)

[Spring Boot](https://start.spring.io)

[React ad Spring WebFlux](https://developer.okta.com/blog/2018/09/25/spring-webflux-websockets-react)

[Spring WebFlux Security Jwt](https://github.com/raphaelDL/spring-webflux-security-jwt)

[Junit 5](https://medium.com/@GalletVictor/migration-from-junit-4-to-junit-5-d8fe38644abe)
