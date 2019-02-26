package com.learning.springboot.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ReactiveMongoMetadataUtil {
    private final ReactiveMongoTemplate mongoTemplate;

    public void recreateCollection(Class<? extends Serializable> entity) {
        mongoTemplate.findAll(entity)
            .collectList()
            .filter(List::isEmpty)
            .subscribe(c -> {
                log.debug("Dropping Collection for entity: {}", entity);
                mongoTemplate.dropCollection(entity)
                    .then(mongoTemplate.createCollection(entity, CollectionOptions.empty()
                    .size(1024 * 1024)
                    .maxDocuments(100)
                    .capped()))
                .subscribe(m -> log.debug("Created Capped Collection for entity: {}", entity));
            });
    }

}
