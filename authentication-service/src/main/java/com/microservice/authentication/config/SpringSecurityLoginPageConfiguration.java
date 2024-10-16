package com.microservice.authentication.config;

import java.io.IOException;

import com.microservice.authentication.service.CustomAuthenticationSuccessHandler;
import com.microservice.authentication.service.CustomOidcUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ott.InMemoryOneTimeTokenService;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.WebauthnConfigurer;
import org.springframework.security.config.annotation.web.configurers.ott.OneTimeTokenLoginConfigurer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.authentication.ott.RedirectOneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.config.annotation.web.configurers.WebauthnConfigurer.webauthn;

@ConditionalOnProperty(prefix = "login", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableWebSecurity
@AllArgsConstructor
@Slf4j
@Order(303)
public class SpringSecurityLoginPageConfiguration {
    private final CustomOidcUserService customOidcUserService;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    private final LogoutSuccessHandler customLogoutSuccessHandler;

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, @Value("${TOKEN_HOST:http://localhost:9998}") String tokenHost)
        throws Exception {
        log.info("SpringSecurityLoginPageConfiguration:defaultSecurityFilterChain");
        return http
            .securityMatcher("/ott/generate", "/webauthn/**", "/login/**", "/*.js", "/*.css", "oauth2/**")
            .authorizeHttpRequests((authorize) -> authorize
                .requestMatchers(SpringSecurityFormConfiguration.WHITELIST).permitAll()
                .anyRequest().authenticated()
            )
            .csrf(c -> c.ignoringRequestMatchers("/webauthn/**", "/ott/generate", "/login/**"))
            .oneTimeTokenLogin(c -> c.tokenGenerationSuccessHandler((request, response, oneTimeToken) -> {
                var msg = String.format("go to %s/login/ott?token=%s", tokenHost, oneTimeToken.getTokenValue());
                System.out.println(msg);
                response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                response.getWriter().print("you've got console mail!");
            }))
            /*.oneTimeTokenLogin(configurer -> configurer.generatedOneTimeTokenSuccessHandler((request, response, oneTimeToken) -> {
                var msg = String.format("go to http://localhost:%s/login/ott?token=%s", port, oneTimeToken.getTokenValue());
                System.out.println(msg);
                response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                response.getWriter().print("you've got console mail!");
            }))*/
            .with(webauthn() ,c -> c
                .allowedOrigins("*")
                .rpId("localhost")
                .rpName("Bootiful Passkeys")
            )
            .oauth2Login(o -> o.successHandler(customAuthenticationSuccessHandler)
                .userInfoEndpoint(u -> u.oidcUserService(customOidcUserService)))
            // Form login handles the redirect to the login page from the
            // authorization server filter chain
            .formLogin(Customizer.withDefaults())
            .logout(l -> l.logoutSuccessUrl("/logout")
                .deleteCookies("SESSIONID")
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", HttpMethod.GET.name()))
                .invalidateHttpSession(true))
            .build();
    }

}
