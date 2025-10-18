package com.microservice.authentication.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users_login_webauthn")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebauthnRegistration implements Serializable {
    @Id
    private String id;

    private List<CredentialRecord> credentialRecords = new ArrayList<>();

    private List<PublicKeyCredentialUserEntity> publicKeyCredentialUserEntities = new ArrayList<>();
}
