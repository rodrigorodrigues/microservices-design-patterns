package com.microservice.authentication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.common.service.SharedAuthenticationService;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.authentication.web.util.CustomDefaultErrorAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security Configuration for form
 */
@Slf4j
@Configuration
@EnableWebSecurity
@AllArgsConstructor
@Order(2)
public class SpringSecurityFormConfiguration extends WebSecurityConfigurerAdapter {
    private final SharedAuthenticationService sharedAuthenticationService;

    private final ObjectMapper objectMapper;

    private final CustomDefaultErrorAttributes customDefaultErrorAttributes;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected UserDetailsService userDetailsService() {
        return sharedAuthenticationService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.debug("SpringSecurityFormConfiguration:Set paths");
        http.requestMatchers()
            .antMatchers("/api/**", "/", "/error")
            .and()
            .authorizeRequests()
            .antMatchers("/error").permitAll()
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .loginProcessingUrl("/api/authenticate").permitAll()
            .successHandler(successHandler())
            .failureHandler(authenticationFailureHandler())
            .and()
            .logout()
            .logoutUrl("/api/logout")
            .deleteCookies("SESSIONID")
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
            .logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", HttpMethod.GET.name()))
            .invalidateHttpSession(true)
            .and()
            .csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(new Http403ForbiddenEntryPoint());
    }

    private AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            if (validateApiPath(request)) {
                Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, exception, true);
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

    /**
     * #      - GF_AUTH_ANONYMOUS_ENABLED=true
     * #      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
     *       - GF_SERVER_ROOT_URL=http://localhost:3000/grafana
     *       - GF_SERVER_SERVE_FROM_SUB_PATH=true
     *       - GF_AUTH_OAUTH_AUTO_LOGIN=true
     *       - GF_AUTH_DISABLE_LOGIN_FORM=true
     *       - GF_AUTH_GENERIC_OAUTH_ENABLED=true
     *       - GF_AUTH_GENERIC_OAUTH_SCOPES=read
     *       - GF_AUTH_GENERIC_OAUTH_ALLOW_SIGN_UP=false
     *       - GF_AUTH_GENERIC_OAUTH_CLIENT_ID=client
     *       - GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET=secret
     *       - GF_AUTH_GENERIC_OAUTH_AUTH_URL=http://localhost:9999/oauth/authorize
     *       - GF_AUTH_GENERIC_OAUTH_TOKEN_URL=http://localhost:9999/oauth/token
     *       - GF_AUTH_GENERIC_OAUTH_API_URL=http://localhost:9999/api/authenticatedUser
     *       - GF_AUTH_GENERIC_OAUTH_ROLE_ATTRIBUTE_PATH=authorities
     *       - GF_AUTH_GENERIC_OAUTH_EMAIL_ATTRIBUTE_PATH=name
     * @return
     */

    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            if (validateApiPath(request)) {
                JwtAccessTokenConverter jwtAccessTokenConverter = getApplicationContext().getBean(JwtAccessTokenConverter.class);

                OAuth2Request oAuth2RequestRequest= new OAuth2Request(null, "client", null, true, null,
                    null, null, null, null);

                OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2RequestRequest, authentication);

                Map<String, String> map = new HashMap<>();
                map.put(DefaultOAuth2AccessToken.ACCESS_TOKEN, UUID.randomUUID().toString());
                map.put(DefaultOAuth2AccessToken.TOKEN_TYPE, DefaultOAuth2AccessToken.BEARER_TYPE);
                map.put(DefaultOAuth2AccessToken.EXPIRES_IN, "30");
                map.put(DefaultOAuth2AccessToken.SCOPE, "read");
                OAuth2AccessToken accessToken = DefaultOAuth2AccessToken.valueOf(map);

                OAuth2AccessToken oAuth2AccessToken = jwtAccessTokenConverter.enhance(accessToken, oAuth2Authentication);

                String authorization = String.format("%s %s", oAuth2AccessToken.getTokenType(), oAuth2AccessToken.getValue());
                JwtTokenDto jwtToken = new JwtTokenDto(authorization);
                response.addHeader(HttpHeaders.AUTHORIZATION, authorization);
                response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.addHeader("sessionId", request.getSession().getId());
                response.setStatus(HttpStatus.OK.value());
                response.getWriter().append(objectMapper.writeValueAsString(jwtToken));
            } else {
                new SavedRequestAwareAuthenticationSuccessHandler().onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

}
