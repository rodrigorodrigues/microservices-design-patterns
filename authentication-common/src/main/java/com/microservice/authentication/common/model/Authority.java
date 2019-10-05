package com.microservice.authentication.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;

@Data
@Document(collection = "authorities")
@NoArgsConstructor
public class Authority implements GrantedAuthority {
    @Id @Indexed
    private String role;

    @JsonCreator
    public Authority(String role) {
        this.role = role;
    }

    @Override @JsonIgnore
    public String getAuthority() {
        return role;
    }
}
