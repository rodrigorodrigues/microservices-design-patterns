FROM maven:3.6.0-jdk-8-alpine

COPY pom.xml ./

# get normal zipkin server
RUN curl -sSL https://zipkin.apache.org/quickstart.sh -o zipkin.sh

# remove --check flag does not work for some reason
RUN sed -i 's/--check//g' zipkin.sh

# install zipkin server
RUN bash zipkin.sh

# download zipkin-server-shared.yml and add eureka configuration
RUN jar xf zipkin.jar BOOT-INF/classes/zipkin-server-shared.yml

RUN echo 'spring.application.name: zipkin-server' >> BOOT-INF/classes/zipkin-server-shared.yml
RUN echo 'eureka.client.enabled: true' >> BOOT-INF/classes/zipkin-server-shared.yml
RUN echo 'eureka.client.serviceUrl.defaultZone: ${EUREKA_SERVER:http://127.0.0.1:8761/eureka/}' >> BOOT-INF/classes/zipkin-server-shared.yml

## download eureka client
#RUN curl -sSL https://start.spring.io/starter.tgz -d dependencies=web,actuator \
#    -d language=groovy -d type=gradle-project -d baseDir=another-project \
#    -o

# build the eureka module
RUN mvn clean install

# rename the jar so it is easier
RUN mv target/eureka-1.0-SNAPSHOT-module.jar eureka.jar

# start zipkin which now has eureka support
CMD java -cp zipkin.jar \
 -Dloader.path='eureka.jar' \
 -Dloader.main=zipkin.server.ZipkinServer org.springframework.boot.loader.PropertiesLauncher \
 --spring.config.location=file:BOOT-INF/classes/zipkin-server-shared.yml \
 --spring.profiles.active=shared \
 -Djava.security.egd=file:/dev/./urandom
