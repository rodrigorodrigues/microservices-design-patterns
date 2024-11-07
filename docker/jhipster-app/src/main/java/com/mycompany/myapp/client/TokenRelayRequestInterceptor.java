package com.mycompany.myapp.client;

import com.mycompany.myapp.security.oauth2.AuthorizationHeaderUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Optional;

public class TokenRelayRequestInterceptor implements RequestInterceptor {

    public static final String AUTHORIZATION = "Authorization";

    private final AuthorizationHeaderUtil authorizationHeaderUtil;

    public TokenRelayRequestInterceptor(AuthorizationHeaderUtil authorizationHeaderUtil) {
        super();
        this.authorizationHeaderUtil = authorizationHeaderUtil;
    }

    @Override
    public void apply(RequestTemplate template) {
        Optional<String> authorizationHeader = authorizationHeaderUtil.getAuthorizationHeader();
        authorizationHeader.ifPresent(s -> template.header(AUTHORIZATION, s));
    }
}
