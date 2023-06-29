package com.microservice.person.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Data
@RequiredArgsConstructor
@Document(collection = "children")
@NoArgsConstructor
public class Child {
    @Id
    private String id = UUID.randomUUID().toString();
    @NonNull
    @NotEmpty
    private String name;
    @NonNull @NotNull
    private LocalDate dateOfBirth;
}
