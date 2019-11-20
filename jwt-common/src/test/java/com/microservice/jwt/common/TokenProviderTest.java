package com.microservice.jwt.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.microservice.jwt.common.config.Java8SpringConfigurationProperties;
import com.microservice.jwt.common.config.Java8SpringConfigurationProperties.Jwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    TokenProvider tokenProvider;

    @Mock
    Java8SpringConfigurationProperties configurationProperties;

    @Mock
    Jwt jwt;

    @BeforeEach
    public void setup() throws Exception {
        when(jwt.getBase64Secret()).thenReturn("TWljcm9zZXJ2aWNlIERlc2lnbiBQYXR0ZXJucyB1c2luZyBtdWx0aXBsZSBsYW5ndWFnZW5zKEphdmEsIE5vZGUsIEtvdGxpbikK");
        when(jwt.getTokenValidityInSeconds()).thenReturn(10L);
        when(jwt.getTokenValidityInSecondsForRememberMe()).thenReturn(20L);
        when(configurationProperties.getJwt()).thenReturn(jwt);
        this.tokenProvider = new TokenProvider(configurationProperties);
        this.tokenProvider.init();
    }

    @Test
    void testCreateToken() {
        UserDetails user = createUser();

        String token = tokenProvider.createToken(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()), "Anonymous", false);

        assertThat(token).isNotEmpty();
    }

    @Test
    void testGetAuthentication() {
        UserDetails user = createUser();

        String token = tokenProvider.createToken(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()), "Anonymous", false);

        PreAuthenticatedAuthenticationToken authentication = tokenProvider.getAuthentication(token);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).containsExactlyInAnyOrder(new SimpleGrantedAuthority("ROLE_PERSON_DELETE"), new SimpleGrantedAuthority("ROLE_PERSON_READ"));
    }

    @Test
    void testValidateTokenShouldReturnTrue() {
        UserDetails user = createUser();

        String token = tokenProvider.createToken(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()), "Anonymous", false);

        boolean validateToken = tokenProvider.validateToken(token);

        assertThat(validateToken).isTrue();
    }

    @Test
    void testValidateTokenShouldReturnFalse() {
        boolean validateToken = tokenProvider.validateToken("Invalid Token");

        assertThat(validateToken).isFalse();
    }

    private UserDetails createUser() {
        return User
            .withUsername("test")
            .password("test")
            .authorities("ROLE_PERSON_READ", "ROLE_PERSON_DELETE")
            .build();
    }

}
