package com.microservice.user.model;

import com.microservice.authentication.common.model.Authority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

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
}
