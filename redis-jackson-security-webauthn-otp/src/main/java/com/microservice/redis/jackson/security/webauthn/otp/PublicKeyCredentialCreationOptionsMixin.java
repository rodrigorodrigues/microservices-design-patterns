package com.microservice.redis.jackson.security.webauthn.otp;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInput;
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.COSEAlgorithmIdentifier;
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInput;
import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInputs;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;

@Slf4j
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PublicKeyCredentialCreationOptionsMixin.PublicKeyCredentialCreationOptionsDeserializer.class)
abstract class PublicKeyCredentialCreationOptionsMixin {
    @JsonCreator
    PublicKeyCredentialCreationOptionsMixin(@JsonProperty("rp") PublicKeyCredentialRpEntity rp, @JsonProperty("user") PublicKeyCredentialUserEntity user,
        @JsonProperty("challenge") Bytes challenge, @JsonProperty("pubKeyCredParams")  List<PublicKeyCredentialParameters> pubKeyCredParams,
        @JsonProperty("timeout") Duration timeout,
        @JsonProperty("excludeCredentials") List<PublicKeyCredentialDescriptor> excludeCredentials,
        @JsonProperty("authenticatorSelection") AuthenticatorSelectionCriteria authenticatorSelection,
        @JsonProperty("attestation") AttestationConveyancePreference attestation,
        @JsonProperty("extensions") ImmutableAuthenticationExtensionsClientInput<Boolean> extensions) {
    }

    public static class PublicKeyCredentialCreationOptionsDeserializer extends JsonDeserializer<PublicKeyCredentialCreationOptions> {

        @Override
        public PublicKeyCredentialCreationOptions deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
            JsonNode treeNode = parser.getCodec().readTree(parser);
            PublicKeyCredentialCreationOptions.PublicKeyCredentialCreationOptionsBuilder keyCredentialCreationOptionsBuilder = PublicKeyCredentialCreationOptions.builder();

            if (treeNode.has("rp")) {
                JsonNode node = treeNode.get("rp");
                PublicKeyCredentialRpEntity.PublicKeyCredentialRpEntityBuilder builder = PublicKeyCredentialRpEntity.builder();
                builder.id(node.get("id").asText());
                builder.name(node.get("name").asText());
                keyCredentialCreationOptionsBuilder.rp(builder.build());
            }

            if (treeNode.has("user")) {
                JsonNode node = treeNode.get("user");
                ImmutablePublicKeyCredentialUserEntity.PublicKeyCredentialUserEntityBuilder builder = ImmutablePublicKeyCredentialUserEntity.builder();
                builder.id(Bytes.fromBase64(node.get("id").asText()));
                builder.name(node.get("name").asText());
                builder.displayName(node.get("displayName").asText());
                keyCredentialCreationOptionsBuilder.user(builder.build());
            }

            if (treeNode.has("challenge")) {
                keyCredentialCreationOptionsBuilder.challenge(Bytes.fromBase64(treeNode.get("challenge").asText()));
            }

            if (treeNode.has("pubKeyCredParams")) {
                ArrayNode node = treeNode.withArray("pubKeyCredParams");
                List<PublicKeyCredentialParameters> list = new ArrayList<>();
                node = (ArrayNode) node.get(1);
                for (JsonNode jsonNode : node) {
                    PublicKeyCredentialType type = PublicKeyCredentialType.valueOf(jsonNode.get("type")
                        .asText());
                    COSEAlgorithmIdentifier alg = Arrays.stream(COSEAlgorithmIdentifier.values()).filter(p -> p.getValue() == jsonNode.get("alg").asInt())
                        .findFirst()
                        .orElse(null);
                    PublicKeyCredentialParameters publicKeyCredentialParameters = publicKeyCredentialParameters(type, alg);
                    if (publicKeyCredentialParameters != null) {
                        list.add(publicKeyCredentialParameters);
                    }
                }
                keyCredentialCreationOptionsBuilder.pubKeyCredParams(list);
            }

            if (treeNode.has("authenticatorSelection")) {
                JsonNode node = treeNode.get("authenticatorSelection");
                AuthenticatorSelectionCriteria.AuthenticatorSelectionCriteriaBuilder builder = AuthenticatorSelectionCriteria.builder();
                builder.residentKey(ResidentKeyRequirement.valueOf(node.get("residentKey").asText()));
                builder.userVerification(userVerificationRequirement(node.get("userVerification").asText()));
                if (node.has("authenticatorAttachment")) {
                    builder.authenticatorAttachment(AuthenticatorAttachment.valueOf(node.get("authenticatorAttachment")
                        .asText()));
                }
                keyCredentialCreationOptionsBuilder.authenticatorSelection(builder.build());
            }

            if (treeNode.has("attestation")) {
                keyCredentialCreationOptionsBuilder.attestation(AttestationConveyancePreference.valueOf(treeNode.get("attestation").asText()));
            }

            if (treeNode.has("timeout")) {
                keyCredentialCreationOptionsBuilder.timeout(Duration.ofSeconds(treeNode.get("timeout").asLong()));
            }

            if (treeNode.has("extensions")) {
                JsonNode node = treeNode.get("extensions");
                ArrayNode arrayNode = node.withArray("inputs");
                List<AuthenticationExtensionsClientInput> inputs = new ArrayList<>();
                arrayNode = (ArrayNode) arrayNode.get(1);
                for (JsonNode jsonNode : arrayNode) {
                    inputs.add(new ImmutableAuthenticationExtensionsClientInput(jsonNode.get("extensionId").asText(), jsonNode.get("input").asBoolean()));
                }
                ImmutableAuthenticationExtensionsClientInputs extensions = new ImmutableAuthenticationExtensionsClientInputs(inputs);
                keyCredentialCreationOptionsBuilder.extensions(extensions);
            }

            return keyCredentialCreationOptionsBuilder.build();
        }

        private final UserVerificationRequirement[] userVerificationRequirements = {UserVerificationRequirement.REQUIRED,
            UserVerificationRequirement.PREFERRED, UserVerificationRequirement.DISCOURAGED};

        private final PublicKeyCredentialParameters[] publicKeyCredentialParameters = {PublicKeyCredentialParameters.EdDSA, PublicKeyCredentialParameters.ES256,
            PublicKeyCredentialParameters.ES256, PublicKeyCredentialParameters.RS1, PublicKeyCredentialParameters.ES384,
            PublicKeyCredentialParameters.ES512, PublicKeyCredentialParameters.RS384, PublicKeyCredentialParameters.RS512};

        public UserVerificationRequirement userVerificationRequirement(String value) {
            return Arrays.stream(userVerificationRequirements).filter(p -> p.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not found value: " + value));
        }

        private PublicKeyCredentialParameters publicKeyCredentialParameters(PublicKeyCredentialType type, COSEAlgorithmIdentifier alg) {
            return Arrays.stream(publicKeyCredentialParameters).filter(p -> p.getType() == type && p.getAlg() == alg)
                .findFirst()
                .orElse(null);
        }
    }
}
