package com.springboot.edgeserver.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.security.oauth2.provider.OAuth2Request;

public class OAuth2RequestModule extends SimpleModule {
    public OAuth2RequestModule() {
        super(OAuth2RequestModule.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(OAuth2Request.class, OAuth2RequestMixin.class);
    }


}
