package com.microservice.redis.jackson.security.webauthn.otp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.springframework.security.web.webauthn.api.AuthenticationExtensionsClientInput;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class AuthenticationExtensionsClientInputsMixin {
    @JsonCreator
    AuthenticationExtensionsClientInputsMixin(@JsonProperty("inputs") List<AuthenticationExtensionsClientInput> inputs) {
    }
}
