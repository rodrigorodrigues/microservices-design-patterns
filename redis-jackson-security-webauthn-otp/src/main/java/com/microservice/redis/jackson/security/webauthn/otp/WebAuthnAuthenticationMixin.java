package com.microservice.redis.jackson.security.webauthn.otp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = WebAuthnAuthenticationMixin.WebAuthnAuthenticationDeserializer.class)
abstract class WebAuthnAuthenticationMixin {
    @JsonCreator
    WebAuthnAuthenticationMixin(@JsonProperty("principal") ImmutablePublicKeyCredentialUserEntity principal,
        @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
    }

    public static class WebAuthnAuthenticationDeserializer extends JsonDeserializer<WebAuthnAuthentication> {

        @Override
        public WebAuthnAuthentication deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            JsonNode treeNode = parser.getCodec().readTree(parser);

            JsonNode principal = treeNode.get("principal");
            ImmutablePublicKeyCredentialUserEntity.PublicKeyCredentialUserEntityBuilder builder = ImmutablePublicKeyCredentialUserEntity.builder();
            builder.id(Bytes.fromBase64(principal.get("id").asText()));
            builder.name(principal.get("name").asText());
            builder.displayName(principal.get("displayName").asText());

            ArrayNode arrayNode = treeNode.withArray("authorities");
            List<GrantedAuthority> authorities = new ArrayList<>();
            arrayNode = (ArrayNode) arrayNode.get(1);
            for (JsonNode jsonNode : arrayNode) {
                if (jsonNode.has("role")) {
                    authorities.add(new SimpleGrantedAuthority(jsonNode.get("role").asText()));
                } else if (jsonNode.has("authority")) {
                    authorities.add(new SimpleGrantedAuthority(jsonNode.get("authority").asText()));
                }
            }

            return new WebAuthnAuthentication(builder.build(), authorities);
        }
    }
}
