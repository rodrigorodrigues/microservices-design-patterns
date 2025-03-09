package com.microservice.authentication.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.microservice.authentication.model.WebauthnRegistration;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MongoUserCredentialRepository implements UserCredentialRepository {
    private final WebauthnRegistrationRepository registrationRepository;
    private final MongoTemplate mongoTemplate;

    public MongoUserCredentialRepository(WebauthnRegistrationRepository registrationRepository, MongoTemplate mongoTemplate) {
        this.registrationRepository = registrationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void delete(Bytes credentialId) {
        log.info("delete:credentialId: {}", credentialId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User is not authenticated cannot delete credentialId: "+credentialId);
            return;
        }
        Optional<WebauthnRegistration> findById = registrationRepository.findById(authentication.getName());
        if (findById.isPresent()) {
            WebauthnRegistration webauthnRegistration = findById.get();
            webauthnRegistration.setCredentialRecords(webauthnRegistration.getCredentialRecords().stream()
                .filter(c -> !c.getCredentialId().equals(credentialId))
                .collect(Collectors.toList()));
            registrationRepository.save(webauthnRegistration);
        }
    }

    @Override
    public void save(CredentialRecord credentialRecord) {
        log.info("save:credentialRecord: {}", credentialRecord);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User is not authenticated cannot create credentialId: "+credentialRecord);
            return;
        }
        String authenticatedUser = authentication.getName();
        Optional<WebauthnRegistration> findById = registrationRepository.findById(authenticatedUser);
        if (findById.isPresent()) {
            WebauthnRegistration webauthnRegistration = findById.get();
            webauthnRegistration.getCredentialRecords().add(credentialRecord);
            registrationRepository.save(webauthnRegistration);
        } else {
            registrationRepository.save(WebauthnRegistration.builder()
                .id(authenticatedUser)
                .credentialRecords(Collections.singletonList(credentialRecord))
                .build());
        }
    }

    @Override
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        try {
            log.info("findByCredentialId: {}", credentialId);
            BasicQuery query = new BasicQuery(String.format("{'credentialRecords.credentialId.bytes': BinData(0, '%s') }", credentialId.toBase64UrlString()
                .replaceAll("_", "/")
                .replaceAll("-", "+")), "{credentialRecords: 1}");
            return mongoTemplate.find(query, WebauthnRegistration.class).stream()
                .flatMap(w -> w.getCredentialRecords().stream())
                .filter(p -> p.getCredentialId().toBase64UrlString().equals(credentialId.toBase64UrlString()))
                .findFirst()
                .orElse(null);
        } catch (IllegalArgumentException iae) {
            log.error("findByCredentialId:Cannot convert to base64", iae);
            return null;
        }
    }

    @Override
    public List<CredentialRecord> findByUserId(Bytes userId) {
        try {
            log.info("findByUserId: {}", userId);
            BasicQuery query = new BasicQuery(String.format("{'credentialRecords.userEntityUserId.bytes': BinData(0, '%s') }", userId.toBase64UrlString()
                .replaceAll("_", "/")
                .replaceAll("-", "+")), "{credentialRecords: 1}");
            return mongoTemplate.find(query, WebauthnRegistration.class).stream()
                .flatMap(w -> w.getCredentialRecords().stream())
                .collect(Collectors.toList());
        } catch (IllegalArgumentException iae) {
            log.error("findByUserId:Cannot convert to base64", iae);
            return Collections.emptyList();
        }
    }
}
