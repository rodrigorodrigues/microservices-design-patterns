package com.microservice.authentication.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.microservice.authentication.common.model.Authentication;
import com.microservice.authentication.common.model.Authority;
import com.microservice.authentication.common.model.UserType;
import com.microservice.authentication.common.repository.AuthenticationCommonRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    private final AuthenticationCommonRepository authenticationCommonRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();
        log.debug("CustomOidcUserService:loadUser:attributes: {}", attributes);
        updateUser(attributes, userRequest);
        return oidcUser;
    }

    private void updateUser(Map<String, Object> attributes, OidcUserRequest userRequest) {
        String email = (String) attributes.get("email");
        Optional<Authentication> user = authenticationCommonRepository.findByEmail(email);
        Authentication authentication = null;
        if(user.isEmpty()) {
            authentication = new Authentication();
        } else {
            authentication = user.get();
        }
        authentication.setEmail(email);
        authentication.setScopes(userRequest.getAccessToken().getScopes());
        authentication.setAuthorities(userRequest.getAccessToken().getScopes().stream().map(Authority::new).collect(Collectors.toList()));
        authentication.setImageUrl((String)attributes.get("picture"));
        authentication.setFullName((String)attributes.get("name"));
        authentication.setUserType(UserType.GOOGLE);
        authenticationCommonRepository.save(authentication);
    }
}
