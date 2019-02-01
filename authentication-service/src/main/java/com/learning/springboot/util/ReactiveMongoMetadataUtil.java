package com.learning.springboot.util;

import com.learning.springboot.constants.DefaultUsers;
import com.learning.springboot.model.Authentication;
import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class ReactiveMongoMetadataUtil {
    private final ReactiveMongoTemplate mongoTemplate;

    private final PasswordEncoder passwordEncoder;

    public Mono<Void> recreateCollection(Class<? extends Serializable> entity) {
        return mongoTemplate.collectionExists(entity)
            .flatMap(m -> mongoTemplate.dropCollection(entity));
    }

    private Mono<MongoCollection<Document>> createCappedCollection(Class<? extends Serializable> entity) {
        return mongoTemplate.createCollection(entity, CollectionOptions.empty()
                .size(1024 * 1024)
                .maxDocuments(100)
                .capped());
    }

    @PostConstruct
    public void init() {
        log.debug("ReactiveMongoMetadataUtil:init: {}", this);

        recreateCollection(Authentication.class).block();
        log.debug("Dropped Collection for entity: {}", Authentication.class);
        createCappedCollection(Authentication.class)
            .subscribe(c -> insertSystemDefaultUser());
    }

    private void insertSystemDefaultUser() {
        log.debug("insertSystemDefaultUser:init");
        Authentication authentication = Authentication.builder()
                .email(DefaultUsers.SYSTEM_DEFAULT.getValue())
                .password(passwordEncoder.encode("noPassword"))
                .fullName("System Authentication")
                .enabled(false)
                .id(UUID.randomUUID().toString())
                .build();
        log.debug("Creating default authentication: {}", authentication);
        mongoTemplate.save(authentication, "users_login")
                .subscribe(u -> log.debug("Created Default Authentication: {}", u));
        log.debug("insertSystemDefaultUser:end");
    }
}
