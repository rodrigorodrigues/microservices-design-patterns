package com.microservice.kotlin.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.server.ResponseStatusException

@ExtendWith(MockitoExtension::class)
internal class CustomDefaultErrorAttributesTest {

    @Test
    fun testGetErrorAttributes() {
        val customDefaultErrorAttributes = CustomDefaultErrorAttributes()

        val request = MockHttpServletRequest()
        var errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, RuntimeException("Connection refused!"), true)

        assertThat(errorAttributes["status"]).isEqualTo(503)
        assertThat(errorAttributes["timestamp"]).isNotNull()
        assertThat(errorAttributes["error"]).isNotNull()
        assertThat(errorAttributes["message"].toString()).contains("Connection refused!")

        errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, BadCredentialsException("user is locked!"), true)
        assertThat(errorAttributes["status"]).isEqualTo(401)

        errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, ResponseStatusException(HttpStatus.NOT_FOUND), true)
        assertThat(errorAttributes["status"]).isEqualTo(404)
    }
}
