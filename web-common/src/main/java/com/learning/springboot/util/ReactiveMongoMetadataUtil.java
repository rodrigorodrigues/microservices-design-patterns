package com.learning.springboot.util;

import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@Slf4j
@Component
@AllArgsConstructor
public class ReactiveMongoMetadataUtil {
    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<MongoCollection<Document>> recreateCollection(Class<? extends Serializable> entity) {
        return mongoTemplate.collectionExists(entity)
                .flatMap(exists -> exists ? mongoTemplate.dropCollection(entity) : Mono.just(exists))
                .then(mongoTemplate.createCollection(entity, CollectionOptions.empty()
                        .size(1024 * 1024)
                        .maxDocuments(100)
                        .capped()));

    }

}
