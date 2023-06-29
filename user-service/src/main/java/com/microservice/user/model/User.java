package com.microservice.user.model;

import com.microservice.authentication.common.model.Authority;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import com.microservice.authentication.common.model.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users_login")
public class User implements Serializable {
    @Id
    private String id;

    @NotEmpty
    @Indexed(unique = true)
    @Email
    private String email;

    @NotEmpty
    private String fullName;

    @NotEmpty
    @Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{4,}$", message = " Password must be at least 4 characters, at least one upper case letter, one lower case letter, and one numeric digit.")
    private String password;

    @NotEmpty @Valid
    private List<Authority> authorities;

    private Boolean enabled = Boolean.TRUE;

    @CreatedBy
    private String createdByUser;

    @CreatedDate
    private Instant createdDate = Instant.now();

    @LastModifiedBy
    private String lastModifiedByUser;

    @LastModifiedDate
    private Instant lastModifiedDate = Instant.now();

    private String imageUrl;

    private UserType userType;
}
