package com.microservice.authentication.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
@EnableAuthorizationServer
@AllArgsConstructor
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter implements ApplicationContextAware {

    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationProperties authenticationProperties;

    private final JwtAccessTokenConverter jwtAccessTokenConverter;

    private final TokenStore tokenStore;

    private ApplicationContext applicationContext;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints.tokenStore(tokenStore)
            .authenticationManager(authenticationManager)
            .accessTokenConverter(jwtAccessTokenConverter);
    }

    private List<String> getListOfServices() {
        try {
            DiscoveryClient eurekaClient = applicationContext.getBean(DiscoveryClient.class);
            return eurekaClient.getServices()
                .stream()
                .map(eurekaClient::getInstances)
                .flatMap(Collection::stream)
                .flatMap(a -> Stream.of(String.format("http://%s:%s/login", a.getHost(), a.getPort()), String.format("http://localhost:%s/login", a.getPort())))
                .collect(Collectors.toList());
        } catch (NoSuchBeanDefinitionException ne) {
            log.warn("Error on method getListOfServices", ne);
            return new ArrayList<>();
        }
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        List<String> listOfServices = getListOfServices();
        setAuthorizeUrls(listOfServices);
        log.debug("listOfServices: {}", listOfServices);
        clients.inMemory()
            .withClient("client")
            .secret(passwordEncoder.encode("secret"))
            .authorizedGrantTypes("password", "authorization_code", "refresh_token")
            .redirectUris(listOfServices.toArray(new String[] {}))
            .autoApprove(true)
            .scopes("read", "write")
            .and()
            .withClient("actuator")
            .secret(passwordEncoder.encode("actuator_password"))
            .authorizedGrantTypes("client_credentials", "password", "authorization_code", "refresh_token")
            .redirectUris(listOfServices.toArray(new String[] {}))
            .autoApprove(true)
            .authorities("ADMIN")
            .scopes("actuator");
    }

    private void setAuthorizeUrls(List<String> listOfServices) {
        List<String> authorizeUrls = authenticationProperties.getAuthorizeUrls();
        authorizeUrls.stream()
            .flatMap(s -> Arrays.stream(s.split(";")))
            .forEach(listOfServices::add);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) {
        oauthServer
            .allowFormAuthenticationForClients()
            .tokenKeyAccess("permitAll()")
            .checkTokenAccess("isAuthenticated()");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
