package com.microservice.authentication.web.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CustomDefaultErrorAttributesTest {

    @Test
    void testGetErrorAttributes() {
        CustomDefaultErrorAttributes customDefaultErrorAttributes = new CustomDefaultErrorAttributes();

        MockHttpServletRequest request = new MockHttpServletRequest();
        String name = DefaultErrorAttributes.class.getName() + ".ERROR";
        request.setAttribute(name, new RuntimeException("Connection refused!"));
        Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.defaults());

        assertThat(errorAttributes.get("status")).isEqualTo(503);
        assertThat(errorAttributes.get("timestamp")).isNotNull();
        assertThat(errorAttributes.get("error")).isNotNull();
        assertThat(errorAttributes.get("message").toString()).contains("Connection refused!");

        request.setAttribute(name, new BadCredentialsException("user is locked!"));
        errorAttributes = customDefaultErrorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.defaults());
        assertThat(errorAttributes.get("status")).isEqualTo(401);

        request.setAttribute(name, new ResponseStatusException(HttpStatus.NOT_FOUND));
        errorAttributes = customDefaultErrorAttributes.getErrorAttributes(new ServletWebRequest(request), ErrorAttributeOptions.defaults());
        assertThat(errorAttributes.get("status")).isEqualTo(404);
    }
}
