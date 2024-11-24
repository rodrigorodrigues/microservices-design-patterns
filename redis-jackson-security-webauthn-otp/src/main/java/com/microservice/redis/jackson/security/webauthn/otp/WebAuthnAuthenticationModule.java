package com.microservice.redis.jackson.security.webauthn.otp;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;

public class WebAuthnAuthenticationModule extends SimpleModule {
    public WebAuthnAuthenticationModule() {
        super(WebAuthnAuthenticationModule.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(WebAuthnAuthentication.class, WebAuthnAuthenticationMixin.class);
    }
}
