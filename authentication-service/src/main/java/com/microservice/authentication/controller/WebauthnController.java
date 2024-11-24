package com.microservice.authentication.controller;

import java.time.Instant;
import java.util.Optional;

import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/webauthns")
@AllArgsConstructor
public class WebauthnController {

    private final AuthenticationCommonRepository authenticationCommonRepository;

    private final PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository;

    private final UserCredentialRepository userCredentialRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CredentialRecordDto[] index(Authentication authentication) {
        String login;
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            login = oidcUser.getName();
        } else {
            Optional<com.microservice.authentication.common.model.Authentication> findById = authenticationCommonRepository.findByEmail(authentication.getName());
            if (findById.isPresent()) {
                com.microservice.authentication.common.model.Authentication authenticationDb = findById.get();
                login = authenticationDb.getUsername();
            } else {
                login = authentication.getName();
            }
        }
        log.debug("Getting passkeys for username: {}", login);
        PublicKeyCredentialUserEntity usernameKeyCredentialUserEntity = publicKeyCredentialUserEntityRepository.findByUsername(login);
        CredentialRecordDto[] credentialRecords = null;
        if (usernameKeyCredentialUserEntity != null) {
            credentialRecords = userCredentialRepository.findByUserId(usernameKeyCredentialUserEntity.getId()).stream()
                .map(CredentialRecordDto::new)
                .toList()
                .toArray(new CredentialRecordDto[] {});
        }
        return credentialRecords;
    }

    public record CredentialRecordDto(String id, String label, Instant created, Instant lastUsed, long signatureCount) {
        public CredentialRecordDto(CredentialRecord credentialRecord) {
            this(credentialRecord.getCredentialId().toBase64UrlString(),
                credentialRecord.getLabel(),
                credentialRecord.getCreated(),
                credentialRecord.getLastUsed(),
                credentialRecord.getSignatureCount());
        }
    }
}
