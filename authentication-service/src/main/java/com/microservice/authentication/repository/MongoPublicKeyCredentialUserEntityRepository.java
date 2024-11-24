package com.microservice.authentication.repository;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import com.microservice.authentication.model.WebauthnRegistration;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnProperty(value = "com.microservice.authentication.redis.enabled", havingValue = "true")
@Component
public class MongoPublicKeyCredentialUserEntityRepository implements PublicKeyCredentialUserEntityRepository {
    private final WebauthnRegistrationRepository registrationRepository;
    private final MongoTemplate mongoTemplate;

    public MongoPublicKeyCredentialUserEntityRepository(WebauthnRegistrationRepository registrationRepository, MongoTemplate mongoTemplate) {
        this.registrationRepository = registrationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes userId) {
        try {
        log.info("PublicKeyCredentialUserEntity findById: {}", userId);
        BasicQuery query = new BasicQuery(String.format("{'publicKeyCredentialUserEntities._id.bytes': BinData(0, '%s') }", userId.toBase64UrlString()
            .replaceAll("_", "/")
            .replaceAll("-", "+")), "{publicKeyCredentialUserEntities: 1}");
        return mongoTemplate.find(query, WebauthnRegistration.class).stream()
            .flatMap(w -> w.getPublicKeyCredentialUserEntities().stream())
            .filter(p -> p.getId().toBase64UrlString().equals(userId.toBase64UrlString()))
            .findFirst()
            .orElse(null);
        } catch (IllegalArgumentException iae) {
            log.error("findById:Cannot convert to base64", iae);
            return null;
        }
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        log.info("PublicKeyCredentialUserEntity findByUsername: {}", username);
        return registrationRepository.findById(username)
            .filter(p -> !p.getPublicKeyCredentialUserEntities().isEmpty())
            .map(WebauthnRegistration::getPublicKeyCredentialUserEntities)
            .flatMap(cr -> cr.stream().filter(key -> key.getName().equals(username)).findFirst())
            .orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
        log.info("save:userEntity: {}", userEntity);
        String authenticatedUser = userEntity.getName();
        Optional<WebauthnRegistration> findById = registrationRepository.findById(authenticatedUser);
        if (findById.isPresent()) {
            WebauthnRegistration webauthnRegistration = findById.get();
            webauthnRegistration.getPublicKeyCredentialUserEntities().add(userEntity);
            registrationRepository.save(webauthnRegistration);
        } else {
            registrationRepository.save(WebauthnRegistration.builder()
                .id(authenticatedUser)
                .publicKeyCredentialUserEntities(Collections.singletonList(userEntity))
                .build());
        }
    }

    @Override
    public void delete(Bytes id) {
        log.info("delete:id: {}", id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User is not authenticated cannot delete id: "+id.toBase64UrlString());
            return;
        }
        Optional<WebauthnRegistration> findById = registrationRepository.findById(authentication.getName());
        if (findById.isPresent()) {
            WebauthnRegistration webauthnRegistration = findById.get();
            webauthnRegistration.setCredentialRecords(webauthnRegistration.getCredentialRecords().stream()
                .filter(c -> !c.getCredentialId().toBase64UrlString().equals(id.toBase64UrlString()))
                .collect(Collectors.toList()));
            registrationRepository.save(webauthnRegistration);
        }
    }
}
