package com.springboot.edgeserver.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;

public class OneTimeTokenAuthenticationTokenModule extends SimpleModule {
    public OneTimeTokenAuthenticationTokenModule() {
        super(OneTimeTokenAuthenticationTokenModule.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(OneTimeTokenAuthenticationToken.class, OneTimeTokenAuthenticationTokenMixin.class);
    }
}
