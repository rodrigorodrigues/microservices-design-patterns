package com.microservice.person.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "persons")
public class Person implements Serializable {
    @Id
    private String id;

    @NotEmpty
    @Size(min = 5, max = 200)
    private String fullName;

    @NotNull
    private LocalDate dateOfBirth;

    @Valid
    private List<Child> children;

    @NotNull @Valid
    private Address address;

    @CreatedBy
    private String createdByUser;

    @CreatedDate
    private Instant createdDate = Instant.now();

    @LastModifiedBy
    private String lastModifiedByUser;

    @LastModifiedDate
    private Instant lastModifiedDate = Instant.now();

    private Boolean activated = true;
}
