package com.learning.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String id;
    private String password;
    private String confirmPassword;
    private String email;
    private String fullName;

    private List<AuthorityDto> authorities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorityDto {
        private String role;
    }

}
