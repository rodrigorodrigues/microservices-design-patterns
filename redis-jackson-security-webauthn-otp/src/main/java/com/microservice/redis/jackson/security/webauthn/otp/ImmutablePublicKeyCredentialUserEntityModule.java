package com.microservice.redis.jackson.security.webauthn.otp;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;

public class ImmutablePublicKeyCredentialUserEntityModule extends SimpleModule {
    public ImmutablePublicKeyCredentialUserEntityModule() {
        super(ImmutablePublicKeyCredentialUserEntityModule.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(ImmutablePublicKeyCredentialUserEntity.class, ImmutablePublicKeyCredentialUserEntityMixin.class);
    }
}
