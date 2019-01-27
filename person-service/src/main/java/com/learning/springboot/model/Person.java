package com.learning.springboot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class Person extends GenericModel {
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
}
