package com.learning.springboot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
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

@Data
@NoArgsConstructor
@Document(collection = "person_user")
public class Person implements UserDetails {
    @Id
    private String id;
    @NotEmpty
    @Size(min = 5, max = 100)
    private String name;
    @NotNull
    private Integer age;
    @Email
    private String email;
    @Valid
    private List<Child> children;
    @NotEmpty @Indexed(unique = true)
    @Size(min = 3, max = 30)
    private String username;
    @NotEmpty
    private String password;
    @NotEmpty @Valid
    private List<Authority> authorities;
    @NotNull @Valid
    private Address address;

    public Person(String name, Integer age, String email, String username, String password, List<Authority> authorities) {
        setName(name);
        setAge(age);
        setEmail(email);
        setUsername(username);
        setPassword(password);
        setAuthorities(authorities);
    }

    @Override
    public Collection<Authority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return username;
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
