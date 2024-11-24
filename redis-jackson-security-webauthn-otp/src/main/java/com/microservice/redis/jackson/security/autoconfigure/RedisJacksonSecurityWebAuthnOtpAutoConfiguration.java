package com.microservice.redis.jackson.security.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.redis.jackson.security.webauthn.otp.AuthenticationExtensionsClientInputModule;
import com.microservice.redis.jackson.security.webauthn.otp.AuthenticationExtensionsClientInputsModule;
import com.microservice.redis.jackson.security.webauthn.otp.AuthenticatorSelectionCriteriaModule;
import com.microservice.redis.jackson.security.webauthn.otp.ImmutableAuthenticationExtensionsClientInputModule;
import com.microservice.redis.jackson.security.webauthn.otp.ImmutableAuthenticationExtensionsClientInputsModule;
import com.microservice.redis.jackson.security.webauthn.otp.ImmutableCredentialRecordModule;
import com.microservice.redis.jackson.security.webauthn.otp.ImmutablePublicKeyCredentialUserEntityModule;
import com.microservice.redis.jackson.security.webauthn.otp.OneTimeTokenAuthenticationTokenModule;
import com.microservice.redis.jackson.security.webauthn.otp.PublicKeyCredentialCreationOptionsModule;
import com.microservice.redis.jackson.security.webauthn.otp.PublicKeyCredentialParametersModule;
import com.microservice.redis.jackson.security.webauthn.otp.PublicKeyCredentialRequestOptionsModule;
import com.microservice.redis.jackson.security.webauthn.otp.PublicKeyCredentialRpEntityModule;
import com.microservice.redis.jackson.security.webauthn.otp.WebAuthnAuthenticationModule;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;

@ConditionalOnProperty(value = "com.microservice.authentication.redis.enabled", havingValue = "true")
@Configuration(proxyBeanMethods = false)
public class RedisJacksonSecurityWebAuthnOtpAutoConfiguration implements BeanClassLoaderAware {
    private ClassLoader loader;

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
        objectMapper.registerModules(new OneTimeTokenAuthenticationTokenModule(),
                new WebauthnJackson2Module(),
                new PublicKeyCredentialRequestOptionsModule(),
                new AuthenticationExtensionsClientInputsModule(),
                new ImmutableAuthenticationExtensionsClientInputsModule(),
                new ImmutableAuthenticationExtensionsClientInputModule(),
                new AuthenticationExtensionsClientInputModule(),
                new PublicKeyCredentialCreationOptionsModule(),
                new PublicKeyCredentialRpEntityModule(),
                new ImmutablePublicKeyCredentialUserEntityModule(),
                new PublicKeyCredentialParametersModule(),
                new AuthenticatorSelectionCriteriaModule(),
                new WebAuthnAuthenticationModule(),
                new ImmutableCredentialRecordModule(),
                new CoreJackson2Module());
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }
}
