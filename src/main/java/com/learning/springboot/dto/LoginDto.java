package com.learning.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

@Data
@ToString(exclude = "password")
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;

    private boolean rememberMe;
}
