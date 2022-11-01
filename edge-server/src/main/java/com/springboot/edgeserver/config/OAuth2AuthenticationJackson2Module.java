package com.springboot.edgeserver.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class OAuth2AuthenticationJackson2Module extends SimpleModule {
    public OAuth2AuthenticationJackson2Module() {
        super(OAuth2AuthenticationJackson2Module.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(OAuth2Authentication.class, OAuth2AuthenticationMixin.class);
    }
}
