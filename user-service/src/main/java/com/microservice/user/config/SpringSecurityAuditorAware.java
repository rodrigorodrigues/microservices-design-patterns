package com.microservice.user.config;

import com.microservice.web.common.util.constants.DefaultUsers;
import lombok.Setter;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Class for auditing mongo entities changes.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Setter
    private Authentication currentAuthenticatedUser;

    /**
     * Return current logged user or default.
     * //TODO Check later how to use {@link org.springframework.security.core.context.ReactiveSecurityContextHolder} with spring webflux.
     * @return current user
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        if (currentAuthenticatedUser != null) {
            return Optional.of(currentAuthenticatedUser)
                .map(Authentication::getName);
        } else {
            return Optional.of(DefaultUsers.SYSTEM_DEFAULT.getValue());
        }
    }

}
