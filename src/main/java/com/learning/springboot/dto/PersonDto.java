package com.learning.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class PersonDto {
    private String id;
    @NotEmpty @Size(min = 5, max = 100)
    private String name;
    @NotNull
    private Integer age;
    @NotEmpty
    private String login;
    @NotEmpty
    private String password;
    @NotEmpty @Valid
    private List<AuthorityDto> authorities;
    @Valid
    private List<ChildrenDto> children;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildrenDto {
        @NotEmpty
        private String name;
        @NotNull
        private Integer age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorityDto {
        @NotEmpty
        private String role;
    }
}
