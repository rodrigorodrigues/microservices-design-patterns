package com.microservice.kotlin.config

import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.*

/**
 * Class for auditing mongo entities changes.
 */
@Component
class SpringSecurityAuditorAware : AuditorAware<String> {

    /**
     * Return current logged user or default.
     * @return current user
     */
    override fun getCurrentAuditor(): Optional<String> {
        return Optional.ofNullable(SecurityContextHolder.getContext().authentication)
            .map { obj: Authentication -> obj.name }
    }

}
