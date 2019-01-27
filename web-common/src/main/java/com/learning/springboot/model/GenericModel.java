package com.learning.springboot.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class GenericModel implements Serializable {
    @CreatedBy @DBRef
    private User createdByUser;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedBy @DBRef
    private User lastModifiedByUser;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
