package com.microservice.redis.jackson.security.webauthn.otp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = ImmutableCredentialRecordMixin.ImmutableCredentialRecordDeserializer.class)
public class ImmutableCredentialRecordMixin {
    @JsonCreator
    ImmutableCredentialRecordMixin(@JsonProperty("credentialType") PublicKeyCredentialType credentialType,
        @JsonProperty("credentialId") Bytes credentialId,
        @JsonProperty("userEntityUserId") Bytes userEntityUserId,
        @JsonProperty("publicKey") PublicKeyCose publicKey,
        @JsonProperty("signatureCount") Long signatureCount,
        @JsonProperty("uvInitialized") Boolean uvInitialized,
        @JsonProperty("transports") Set<AuthenticatorTransport> transports,
        @JsonProperty("backupEligible") Boolean backupEligible,
        @JsonProperty("backupState") Boolean backupState,
        @JsonProperty("attestationObject") Bytes attestationObject,
        @JsonProperty("attestationClientDataJSON") Bytes attestationClientDataJSON,
        @JsonProperty("created") Instant created,
        @JsonProperty("lastUsed") Instant lastUsed,
        @JsonProperty("label") String label) {
    }

    public static class ImmutableCredentialRecordDeserializer extends JsonDeserializer<ImmutableCredentialRecord> {

        @Override
        public ImmutableCredentialRecord deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            JsonNode treeNode = parser.getCodec().readTree(parser);

            ImmutableCredentialRecord.ImmutableCredentialRecordBuilder builder = ImmutableCredentialRecord.builder();

            if (treeNode.has("credentialType")) {
                builder.credentialType(PublicKeyCredentialType.valueOf(treeNode.get("credentialType").asText()));
            }

            if (treeNode.has("credentialId")) {
                builder.credentialId(Bytes.fromBase64(treeNode.get("credentialId").asText()));
            }

            if (treeNode.has("userEntityUserId")) {
                builder.userEntityUserId(Bytes.fromBase64(treeNode.get("userEntityUserId").asText()));
            }

            if (treeNode.has("publicKey")) {
                JsonNode node = treeNode.get("publicKey");
                builder.publicKey(new ImmutablePublicKeyCose(node.get("bytes").asText().getBytes(StandardCharsets.UTF_8)));
            }

            if (treeNode.has("signatureCount")) {
                builder.signatureCount(treeNode.get("signatureCount").asLong());
            }

            if (treeNode.has("uvInitialized")) {
                builder.uvInitialized(treeNode.get("uvInitialized").asBoolean());
            }

            if (treeNode.has("backupEligible")) {
                builder.backupEligible(treeNode.get("backupEligible").asBoolean());
            }

            if (treeNode.has("backupState")) {
                builder.backupState(treeNode.get("backupState").asBoolean());
            }

            if (treeNode.has("attestationObject")) {
                builder.attestationObject(Bytes.fromBase64(treeNode.get("attestationObject").asText()));
            }

            if (treeNode.has("created")) {
                builder.created(Instant.parse(treeNode.get("created").asText()));
            }

            if (treeNode.has("lastUsed")) {
                builder.lastUsed(Instant.parse(treeNode.get("lastUsed").asText()));
            }

            if (treeNode.has("label")) {
                builder.label(treeNode.get("label").asText());
            }

            return builder.build();
        }
    }
}
