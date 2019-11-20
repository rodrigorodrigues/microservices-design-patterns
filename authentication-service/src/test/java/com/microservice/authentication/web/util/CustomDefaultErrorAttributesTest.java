package com.microservice.authentication.web.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CustomDefaultErrorAttributesTest {

    @Test
    void testGetErrorAttributes() {
        CustomDefaultErrorAttributes customDefaultErrorAttributes = new CustomDefaultErrorAttributes();

        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, new RuntimeException("Connection refused!"), true);

        assertThat(errorAttributes.get("status")).isEqualTo(503);
        assertThat(errorAttributes.get("timestamp")).isNotNull();
        assertThat(errorAttributes.get("error")).isNotNull();
        assertThat(errorAttributes.get("message").toString()).contains("Connection refused!");

        errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, new BadCredentialsException("user is locked!"), true);
        assertThat(errorAttributes.get("status")).isEqualTo(401);

        errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, new ResponseStatusException(HttpStatus.NOT_FOUND), true);
        assertThat(errorAttributes.get("status")).isEqualTo(404);
    }
}
