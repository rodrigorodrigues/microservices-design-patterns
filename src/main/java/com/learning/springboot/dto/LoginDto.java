package com.learning.springboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@ToString(exclude = "password")
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {

    @NotEmpty
    @Size(min = 1, max = 50)
    private String username;

    @NotEmpty
    @Size(min = 4, max = 100)
    private String password;

    private boolean rememberMe;
}
