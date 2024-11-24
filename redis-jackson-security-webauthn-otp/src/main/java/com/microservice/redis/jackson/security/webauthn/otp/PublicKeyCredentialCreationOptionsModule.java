package com.microservice.redis.jackson.security.webauthn.otp;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;

public class PublicKeyCredentialCreationOptionsModule extends SimpleModule {
    public PublicKeyCredentialCreationOptionsModule() {
        super(PublicKeyCredentialCreationOptionsModule.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(PublicKeyCredentialCreationOptions.class, PublicKeyCredentialCreationOptionsMixin.class);
    }
}
