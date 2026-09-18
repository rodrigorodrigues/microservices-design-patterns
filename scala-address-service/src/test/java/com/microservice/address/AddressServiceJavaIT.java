package com.microservice.address;

import com.microservice.address.auth.JwtDirectives;
import com.microservice.address.models.Address;
import com.microservice.address.repository.AddressRepository;
import com.microservice.address.routes.AddressRoutes;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.apache.pekko.http.javadsl.server.directives.RouteAdapter;
import org.bson.Document;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.HmacKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpHeaders;
import scala.Option;
import scala.concurrent.ExecutionContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ScalaAddressServiceConfig.class, properties = {
    "spring.cloud.consul.config.enabled=false",
    "spring.cloud.consul.discovery.enabled=false",
    "spring.cloud.consul.enabled=false",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.data.mongodb.repositories.type=auto",
    "spring.data.mongodb.auto-index-creation=true",
    "spring.data.mongodb.repositories.enabled=true",
    "com.microservice.authentication.jwt.key-value=mytestsecret-very-long-secret-to-satisfy-jose4j-validation"
})
@Import(TestcontainersConfigurationJava.class)
public class AddressServiceJavaIT {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private String jwtSecret = "mytestsecret-very-long-secret-to-satisfy-jose4j-validation";

    private static ActorSystem addressServerSystem;
    private static ServerBinding addressServerBinding;
    private static int addressServerPort;

    @BeforeEach
    public void setup() throws Exception {
        // Clear repository
        mongoTemplate.dropCollection(Address.class).block();

        startAddressServerOnce();
    }

    private synchronized void startAddressServerOnce() throws Exception {
        if (addressServerSystem != null) {
            return;
        }
        addressServerSystem = ActorSystem.create("address-service-it");
        ExecutionContext ec = addressServerSystem.dispatcher();
        JwtDirectives jwtDirectives = new JwtDirectives(Option.empty(), Option.apply(jwtSecret), false);
        AddressRoutes addressRoutes = new AddressRoutes(addressRepository, jwtDirectives, ec);

        addressServerBinding = Http.get(addressServerSystem)
            .newServerAt("localhost", 0)
            .bind(RouteAdapter.asJava(addressRoutes.routes()))
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);
        addressServerPort = addressServerBinding.localAddress().getPort();
    }

    @AfterAll
    public static void tearDownAddressServer() throws Exception {
        if (addressServerBinding != null) {
            addressServerBinding.unbind().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
        if (addressServerSystem != null) {
            addressServerSystem.terminate();
        }
    }

    @Test
    public void verifyContextStartsAndDatabaseIsAccessible() {
        Document doc = new Document().append("street", "Main St").append("city", "Dublin");
        mongoTemplate.save(doc, "addresses").block();
        List<Document> addresses = mongoTemplate.findAll(Document.class, "addresses").collectList().block();
        assertThat(addresses).hasSize(1);
    }

    @Test
    public void shouldCreateAndRetrieveAddress() throws Exception {
        Document doc = new Document()
            .append("street", "123 Test St")
            .append("city", "Test City")
            .append("state", "Test State")
            .append("zipCode", "99999")
            .append("country", "Test Country");
        
        Document saved = mongoTemplate.save(doc, "addresses").block();
        String id = saved.getObjectId("_id").toString();
        
        Document found = mongoTemplate.findById(id, Document.class, "addresses").block();
        assertThat(found).isNotNull();
        assertThat(found.getString("street")).isEqualTo("123 Test St");
    }

    @Test
    public void shouldGetAllAddresses() {
        Document doc1 = new Document().append("street", "Street 1").append("city", "City 1").append("state", "State 1").append("zipCode", "111").append("country", "Country 1");
        Document doc2 = new Document().append("street", "Street 2").append("city", "City 2").append("state", "State 2").append("zipCode", "222").append("country", "Country 2");
        
        mongoTemplate.save(doc1, "addresses").block();
        mongoTemplate.save(doc2, "addresses").block();
        
        List<Document> all = mongoTemplate.findAll(Document.class, "addresses").collectList().block();
        assertThat(all).hasSize(2);
    }

    @Test
    public void shouldUpdateAddress() {
        Document doc = new Document().append("street", "Original Street").append("city", "City").append("state", "State").append("zipCode", "ZIP").append("country", "Country");
        Document savedDoc = mongoTemplate.save(doc, "addresses").block();
        String id = savedDoc.getObjectId("_id").toString();
        
        savedDoc.put("street", "Updated Street");
        mongoTemplate.save(savedDoc, "addresses").block();
        
        Document updated = mongoTemplate.findById(id, Document.class, "addresses").block();
        assertThat(updated.getString("street")).isEqualTo("Updated Street");
    }

    @Test
    public void shouldDeleteAddress() {
        Document doc = new Document().append("street", "To Delete").append("city", "City").append("state", "State").append("zipCode", "ZIP").append("country", "Country");
        Document savedDoc = mongoTemplate.save(doc, "addresses").block();
        String id = savedDoc.getObjectId("_id").toString();
        
        assertThat(mongoTemplate.findById(id, Document.class, "addresses").block()).isNotNull();
        
        mongoTemplate.remove(savedDoc, "addresses").block();
        
        assertThat(mongoTemplate.findById(id, Document.class, "addresses").block()).isNull();
    }
    private String generateToken(String subject, List<String> roles) throws Exception {
        JwtClaims claims = new JwtClaims();
        claims.setSubject(subject);
        claims.setStringListClaim("authorities", roles);
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(2);
        claims.setIssuer("jwt");

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(new HmacKey(jwtSecret.getBytes()));
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);

        return "Bearer " + jws.getCompactSerialization();
    }

    @Test
    public void shouldReturn401WhenNoTokenProvided() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + addressServerPort + "/api/addresses"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    public void shouldReturn403WhenInsufficientRoles() throws Exception {
        String token = generateToken("user-1", Arrays.asList("ROLE_PERSON_READ"));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + addressServerPort + "/api/addresses"))
            .header(HttpHeaders.AUTHORIZATION, token)
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
