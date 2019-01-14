## Java 8 + Spring Boot 2 + WebFlux + React

### Prerequisites
 * JDK 1.8
 * Maven

### Install

`mvn clean package`

### Run Spring Boot

`mvn spring-boot:run` and access http://localhost:8080

Default users are:

```
admin/password

master/password123

test/test
```

### Run React Web app

```
cd react-webapp

sudo npm install

sudo npm start
```

### Swagger
[Swagger](http://localhost:8080/swagger-ui.html)

### TODO List

* [X] Java - Split Person and User in different entities
* [ ] Java - Add Cloud Foundry for deploy
* [ ] Java - Add Test for Users Classes
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
