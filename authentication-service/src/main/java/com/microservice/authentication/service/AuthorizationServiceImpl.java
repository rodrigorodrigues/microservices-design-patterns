package com.microservice.authentication.service;

import com.microservice.authentication.common.service.SharedAuthenticationServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.joining;

@Service
@AllArgsConstructor
public class AuthorizationServiceImpl implements ClientDetailsService {

    private final SharedAuthenticationServiceImpl userDetailsService;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        return Optional.ofNullable(userDetailsService.loadUserByUsername(clientId))
            .filter(isUserEnabled())
            .map(u -> {
                String authorities = u.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(joining(","));
                String grantTypes = String.join(",", "implicit", "refresh_token", "password", "authorization_code", "client_credentials");
                BaseClientDetails baseClientDetails = new BaseClientDetails(u.getUsername(), null, "any", grantTypes, authorities);
                baseClientDetails.setClientSecret(u.getPassword());
                return baseClientDetails;
            })
            .orElseThrow(() -> new LockedException(String.format("User(%s) is locked", clientId)));
    }

    private Predicate<UserDetails> isUserEnabled() {
        return u -> u.isEnabled() && u.isAccountNonExpired() && u.isAccountNonLocked() && u.isCredentialsNonExpired();
    }
}
