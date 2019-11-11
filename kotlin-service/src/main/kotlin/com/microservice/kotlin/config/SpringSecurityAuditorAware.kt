package com.microservice.kotlin.config

import com.microservice.web.common.util.constants.DefaultUsers
import org.springframework.data.domain.AuditorAware
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
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated) {
            Optional.of(authentication.name)
        } else {
            Optional.of(DefaultUsers.SYSTEM_DEFAULT.value)
        }
    }

}
