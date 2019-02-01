package com.learning.springboot.config;

import com.learning.springboot.constants.DefaultUsers;
import com.learning.springboot.model.Authentication;
import lombok.Setter;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Class for auditing mongo entities changes.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Setter
    private Authentication currentAuthenticatedAuthentication;

    /**
     * Return current logged user or default.
     * //TODO Check later how to use {@link org.springframework.security.core.context.ReactiveSecurityContextHolder} with spring webflux.
     * @return current user
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        if (currentAuthenticatedAuthentication != null) {
            return Optional.of(currentAuthenticatedAuthentication)
                .map(u -> u.getUsername() + " - " + u.getFullName());
        } else {
            return Optional.of(DefaultUsers.SYSTEM_DEFAULT.getValue());
        }
    }

}
