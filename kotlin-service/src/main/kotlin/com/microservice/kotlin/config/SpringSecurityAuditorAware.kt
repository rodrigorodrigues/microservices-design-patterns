package com.microservice.kotlin.config

import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
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
            .map { a: Authentication ->
                if (a is OAuth2AuthenticationToken) {
                    val oidcUser = a.principal as DefaultOidcUser
                    return@map oidcUser.email
                } else {
                    return@map a.name
                }
            }
    }

}
