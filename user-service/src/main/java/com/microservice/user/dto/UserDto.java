package com.microservice.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.microservice.authentication.common.model.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private String id;
    private String currentPassword;
    private String password;
    private String confirmPassword;
    private String email;
    private String fullName;
    private String createdByUser;
    private Instant createdDate;
    private String lastModifiedByUser;
    private Instant lastModifiedDate;
    private Boolean activated;
    private UserType userType;
    private String imageUrl;

    private List<AuthorityDto> authorities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorityDto {
        private String role;
    }

}
