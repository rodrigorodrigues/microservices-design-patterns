package com.microservice.authentication.common.repository;

import com.microservice.authentication.common.model.Authentication;
import org.springframework.data.repository.Repository;

public interface AuthenticationCommonRepository extends Repository<Authentication, String> {
    Authentication findByEmail(String email);
    Authentication findById(String id);
    Authentication save(Authentication authentication);
}
