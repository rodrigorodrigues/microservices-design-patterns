package com.learning.springboot.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@RequiredArgsConstructor
@Document(collection = "persons")
public class Person {
    @Id
    private String id;
    @NonNull
    @NotNull
    @Size(min = 5, max = 100)
    private String name;
    @NonNull @NotNull
    private Integer age;
    @NonNull @DBRef
    private List<Child> children;
}
