package com.microservice.authentication.util;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.web.common.util.constants.DefaultUsers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class MongoRecreateCollectionUtil {
    private final MongoTemplate mongoTemplate;

    private final PasswordEncoder passwordEncoder;

    private void recreateCollection(Class<? extends Serializable> entity) {
        if (CollectionUtils.isEmpty(mongoTemplate.findAll(entity))) {
            log.debug("Dropping Collection for entity: {}", entity);
            mongoTemplate.dropCollection(entity);
            createCappedCollection(entity);
        }
        insertSystemDefaultUser();
    }

    private void createCappedCollection(Class<? extends Serializable> entity) {
        mongoTemplate.createCollection(entity, CollectionOptions.empty()
            .size(1024 * 1024)
            .maxDocuments(100)
            .capped());
    }

    @PostConstruct
    public void init() {
        log.debug("ReactiveMongoMetadataUtil:init: {}", this);

        recreateCollection(Authentication.class);
    }

    private void insertSystemDefaultUser() {
        if (!mongoTemplate.exists(new BasicQuery(String.format("{ email : '%s' }", DefaultUsers.SYSTEM_DEFAULT.getValue())), Authentication.class)) {
            log.debug("insertSystemDefaultUser:init");
            Authentication authentication = Authentication.builder()
                .email(DefaultUsers.SYSTEM_DEFAULT.getValue())
                .password(passwordEncoder.encode("noPassword"))
                .fullName("System Administrator")
                .enabled(false)
                .id(UUID.randomUUID().toString())
                .build();
            log.debug("Creating default authentication: {}", authentication);
            authentication = mongoTemplate.save(authentication, "users_login");
            log.debug("Created Default Authentication: {}", authentication);
            log.debug("insertSystemDefaultUser:end");
        }
    }
}
