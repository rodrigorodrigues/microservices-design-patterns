package com.microservice.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Object to return as body in JWT Authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtTokenDto implements Serializable {
    @JsonProperty("id_token")
    private String idToken;
}
