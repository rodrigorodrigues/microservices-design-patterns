package com.springboot.configserver.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Check if request has 'X-Encrypt-Key' Header or Parameter.
 */
@Slf4j
@Component
@AllArgsConstructor
public class WebSecurity {

    private final ConfigServerProperties configServerProperties;

    private final TextEncryptor textEncryptor;

    public static final String ENCRYPT_KEY_HEADER = "X-Encrypt-Key";

    /**
     * Returns true if request has 'X-Encrypt-Key' Header or Parameter.
     * Also returns true if user is authenticated and has role 'ADMIN'.
     * @param request current http request
     * @param authentication current authenticated user
     * @return boolean
     */
    public boolean checkEncryptKey(HttpServletRequest request, Authentication authentication) {
        try {
            String encryptHeader = request.getHeader(ENCRYPT_KEY_HEADER);
            String encryptParameter = request.getParameter(ENCRYPT_KEY_HEADER);
            log.debug("encryptParameter:before: {}", encryptParameter);
            String servletPath = request.getServletPath();
            encryptParameter = extractEncryptFromQueryString(encryptParameter, servletPath);
            if (StringUtils.isNotBlank(encryptParameter)) {
                encryptParameter = encryptParameter.replaceFirst(servletPath, "");
            }
            log.debug("encryptHeader: {}\tservletPath: {}\tencryptParameter:after: {}", encryptHeader, servletPath, encryptParameter);
            if (StringUtils.isNotBlank(encryptHeader)) {
                return configServerProperties.getPassword().equals(textEncryptor.decrypt(encryptHeader));
            } else if (StringUtils.isNotBlank(encryptParameter)) {
                return configServerProperties.getPassword().equals(textEncryptor.decrypt(encryptParameter));
            } else {
                return authentication.isAuthenticated() &&
                    authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals("ROLE_ADMIN"));
            }
        } catch (Exception e) {
            log.debug("Unexpected error:", e);
            return false;
        }
    }

    private String extractEncryptFromQueryString(String encryptParameter, String servletPath) {
        if (StringUtils.isBlank(encryptParameter) && servletPath.contains("?")) {
            Optional<String> param = Stream.of(servletPath.substring(servletPath.indexOf("?") + 1).split("&"))
                .filter(p -> p.startsWith(ENCRYPT_KEY_HEADER))
                .findFirst();
            if (param.isPresent()) {
                return param.map(p -> p.substring(p.indexOf(ENCRYPT_KEY_HEADER) + ENCRYPT_KEY_HEADER.length() + 1))
                    .get();
            } else {
                return encryptParameter;
            }
        } else {
            return encryptParameter;
        }
    }
}
