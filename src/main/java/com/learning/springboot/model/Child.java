package com.learning.springboot.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@RequiredArgsConstructor
@Document(collection = "children")
public class Child {
    @Id
    private String id;
    @NonNull
    @NotNull
    @Size(min = 5, max = 100)
    private String name;
    @NonNull @NotNull
    private Integer age;
}
