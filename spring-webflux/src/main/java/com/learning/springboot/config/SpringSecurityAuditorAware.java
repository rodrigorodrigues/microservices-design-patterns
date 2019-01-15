package com.learning.springboot.config;

import com.learning.springboot.model.User;
import com.learning.springboot.service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Class for auditing mongo entities changes.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<User> {

    private final UserService userService;

    @Setter @Getter
    private User currentAuthenticatedUser;

    public SpringSecurityAuditorAware(UserService userService) {
        this.userService = userService;
    }

    /**
     * Return current logged user or default.
     * //TODO Check later how to use {@link org.springframework.security.core.context.ReactiveSecurityContextHolder} with spring webflux.
     * @return current user
     */
    @Override
    public Optional<User> getCurrentAuditor() {
        if (currentAuthenticatedUser != null) {
            return Optional.of(currentAuthenticatedUser);
        } else {
            return getSystemDefaultUser();
        }
    }

    private Optional<User> getSystemDefaultUser() {
        return userService.findSystemDefaultUser().blockOptional();
    }
}