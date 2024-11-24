package com.microservice.redis.jackson.security.webauthn.otp;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class OneTimeTokenAuthenticationTokenMixin {
    @JsonCreator
    OneTimeTokenAuthenticationTokenMixin(@JsonProperty("principal") Authentication principal,
        @JsonProperty("tokenValue") String tokenValue) {
    }

    @JsonCreator
    OneTimeTokenAuthenticationTokenMixin(@JsonProperty("tokenValue") String tokenValue) {
    }

    @JsonCreator
    OneTimeTokenAuthenticationTokenMixin(@JsonProperty("principal") Authentication principal,
        @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
    }
}
