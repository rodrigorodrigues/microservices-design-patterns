package com.microservice.authentication.config;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.microservice.authentication.service.CustomAuthenticationSuccessHandler;
import com.microservice.authentication.service.CustomOidcUserService;
import com.microservice.web.common.util.CustomDefaultErrorAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.session.RedisSessionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.web.context.request.ServletWebRequest;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SpringSecurityConfiguration {
    private final ObjectMapper objectMapper;

    private final CustomDefaultErrorAttributes customDefaultErrorAttributes;

    private final JwtDecoder jwtDecoder;

    private final JwtEncoder jwtEncoder;

    private final CustomOidcUserService customOidcUserService;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    private final LogoutSuccessHandler customLogoutSuccessHandler;

    private final AuthenticationProperties properties;

    private final RedisSessionProperties redisSessionProperties;

    private final SessionRepository sessionRepository;

    static final String[] WHITELIST = {
        // -- swagger ui
        "/v3/api-docs/**",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui.html",
        "/webjars/**",
        "/*.js",
        "/*.css",
        "/*.html",
        "/favicon.ico",
        // other public endpoints of your API may be appended to this array
        "/actuator/info",
        "/actuator/health/**",
        "/actuator/prometheus",
        "/error",
        "/.well-known/**",
        "/api/refreshToken",
        "/api/csrf",
        "/ott/generate",
        "/webauthn/**",
        "oauth2/**",
        "/connect/**",
        "/userInfo"
    };

    @Order(1)
    @Bean
    public SecurityFilterChain oauth2ServerSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("oauth2ServerSecurityFilterChain");

/*
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointMatchers = authorizationServerConfigurer.getEndpointsMatcher();


        http
            .securityMatcher(new OrRequestMatcher(endpointMatchers))
            // Redirect to the login page when not authenticated from the
            // authorization endpoint
            .exceptionHandling((exceptions) -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
            .formLogin(withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers(endpointMatchers))
            .apply(authorizationServerConfigurer);
        return http
            // Accept access tokens for User Info and/or Client Registration
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(withDefaults()))
            .build();
*/

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(oidc -> oidc.clientRegistrationEndpoint(Customizer.withDefaults()));

        return http
            // Redirect to the login page when not authenticated from the
            // authorization endpoint
            .exceptionHandling((exceptions) -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
            // Accept access tokens for User Info and/or Client Registration
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
            .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain authenticateSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("authenticateSecurityFilterChain");
        return http
            .securityMatcher("/api/**", "/", "/error", "/actuator/**")
            .authorizeHttpRequests(a -> a.requestMatchers(WHITELIST).permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginProcessingUrl("/api/authenticate").permitAll()
                .successHandler(successHandler())
                .failureHandler(authenticationFailureHandler()))
            .logout(l -> l.logoutUrl("/api/logout")
                .deleteCookies("SESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpStatus.OK.value());
                    response.getWriter().flush();
                })
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", HttpMethod.GET.name()))
                .invalidateHttpSession(true))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.NEVER))
            .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .exceptionHandling(e -> e.authenticationEntryPoint(this::handleErrorResponse))
            .oauth2ResourceServer(o -> o.accessDeniedHandler(this::handleErrorResponse)
                .authenticationEntryPoint(this::handleErrorResponse)
                .jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .cors(Customizer.withDefaults())
            .build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain loginSecurityFilterChain(HttpSecurity http, @Value("${TOKEN_HOST:http://localhost:9998}") String tokenHost)
        throws Exception {
        log.info("loginSecurityFilterChain");
        return http
            .securityMatcher("/ott/generate", "/webauthn/**", "/login/**", "/*.js", "/*.css", "/oauth2/**", "/connect/register")
            .authorizeHttpRequests((authorize) -> authorize
                .requestMatchers(WHITELIST).permitAll()
                .anyRequest().authenticated()
            )
            //.csrf(c -> c.ignoringRequestMatchers("/connect/register", "/oauth2/token"))
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
            .webAuthn(c -> c
                .allowedOrigins("*")
                .rpId("localhost")
                .rpName("Bootiful Passkeys")
            )
            .oauth2Login(o -> o.successHandler(successHandler())
                .userInfoEndpoint(u -> u.oidcUserService(customOidcUserService)))
            // Form login handles the redirect to the login page from the
            // authorization server filter chain
            .formLogin(c -> c.successHandler(successHandler())
                .failureHandler(authenticationFailureHandler()))
            .logout(l -> l.logoutSuccessUrl("/logout")
                .deleteCookies("SESSIONID")
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", HttpMethod.GET.name()))
                .invalidateHttpSession(true))
            .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(RegistrationProperties props) {
        RegisteredClient registrarClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(props.registrarClientId())
            .clientSecret(props.registrarClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .clientSettings(ClientSettings.builder()
                .requireProofKey(false)
                .requireAuthorizationConsent(false)
                .build())
            .scope("client.create")
            .scope("client.read")
            .build();

        RegisteredClientRepository delegate = new InMemoryRegisteredClientRepository(registrarClient);
        return new CustomRegisteredClientRepository(delegate);
    }

    @ConfigurationProperties(prefix = "baeldung.security.server.registration")
    public record RegistrationProperties (
        String registrarClientId,
        String registrarClientSecret
    ) {}

    private void handleErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        log.error("Exception Handler - Error response", exception);
        HttpStatusCode status = customDefaultErrorAttributes.getHttpStatusError(exception);
        Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.defaults());
        errorAttributes.put("message", exception.getLocalizedMessage());
        errorAttributes.put("status", status.value());
        response.setStatus(status.value());
        response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().append(objectMapper.writeValueAsString(errorAttributes));
    }

    private AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            log.error("API - Error response", exception);
            request.setAttribute(DefaultErrorAttributes.class.getName() + ".ERROR", exception);
            if (validateApiPath(request)) {
                Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.defaults());
                response.setStatus(Integer.parseInt(errorAttributes.get("status").toString()));
                response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().append(objectMapper.writeValueAsString(errorAttributes));
            } else {
                new SimpleUrlAuthenticationFailureHandler().onAuthenticationFailure(request, response, exception);
            }
        };
    }

    private boolean validateApiPath(HttpServletRequest request) {
        return StringUtils.isNotBlank(request.getPathInfo()) && request.getPathInfo().startsWith("/api/") ||
            StringUtils.isNotBlank(request.getServletPath()) && request.getServletPath().startsWith("/api/");
    }

    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            Jwt jwt = generateToken(authentication);
            OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), new HashSet<>(jwt.getClaimAsStringList("scopes")));
            String sessionId = request.getSession().getId();
            Session session = sessionRepository.findById(sessionId);
            if (session == null) {
                session = sessionRepository.createSession();
                sessionId = session.getId();
            }
            log.debug("sessionId: {}", sessionId);
            session.setAttribute("token", token);
            sessionRepository.save(session);
            if (validateApiPath(request)) {
                response.addHeader(HttpHeaders.AUTHORIZATION, String.format("%s %s", token.getTokenType().getValue(), token.getTokenValue()));
                response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.addHeader("sessionId", sessionId);
                response.setStatus(HttpStatus.OK.value());
                response.getWriter().append(objectMapper.writeValueAsString(token));
            } else {
                new SavedRequestAwareAuthenticationSuccessHandler().onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

    private Jwt generateToken(org.springframework.security.core.Authentication authentication) {
        Instant now = Instant.now();
        long expiry = 36000L;
        Set<String> scopes = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.getIssuer())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiry))
            .subject(authentication.getName())
            .claim("scopes", scopes)
            .claim("authorities", scopes)
            .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims));
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
