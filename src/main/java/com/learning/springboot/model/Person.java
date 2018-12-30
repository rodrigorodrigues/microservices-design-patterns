package com.learning.springboot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
@Document(collection = "person_user")
public class Person implements UserDetails {
    @Id
    private String id = UUID.randomUUID().toString();
    @NonNull
    @NotEmpty
    @Size(min = 5, max = 100)
    private String name;
    @NonNull @NotNull
    private Integer age;
    @NonNull @Email
    private String email;
    @Valid
    private List<Child> children;
    @NotEmpty @NonNull @Indexed
    private String login;
    @NotEmpty @NonNull
    private String password;
    @NotEmpty @NonNull @Valid
    private List<Authority> authorities;
    @NotNull @Valid
    private Address address;

    @Override
    public Collection<Authority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
}
