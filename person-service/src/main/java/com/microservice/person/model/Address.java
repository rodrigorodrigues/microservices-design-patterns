package com.microservice.person.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotEmpty;
import java.util.UUID;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
@Document(collection = "address")
public class Address {
    @Id
    private String id = UUID.randomUUID().toString();
    @NonNull @NotEmpty
    private String address;
    @NonNull @NotEmpty
    private String city;
    @NonNull @NotEmpty
    private String stateOrProvince;
    @NonNull @NotEmpty
    private String country;
    @NonNull @NotEmpty
    private String postalCode;
}
