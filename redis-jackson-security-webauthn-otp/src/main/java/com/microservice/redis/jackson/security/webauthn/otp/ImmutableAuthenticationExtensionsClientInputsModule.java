package com.microservice.redis.jackson.security.webauthn.otp;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.security.web.webauthn.api.ImmutableAuthenticationExtensionsClientInputs;

public class ImmutableAuthenticationExtensionsClientInputsModule extends SimpleModule {
    public ImmutableAuthenticationExtensionsClientInputsModule() {
        super(ImmutableAuthenticationExtensionsClientInputsModule.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(ImmutableAuthenticationExtensionsClientInputs.class, ImmutableAuthenticationExtensionsClientInputsMixin.class);
    }
}
