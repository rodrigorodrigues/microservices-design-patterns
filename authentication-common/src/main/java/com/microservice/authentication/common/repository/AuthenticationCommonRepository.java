package com.microservice.authentication.common.repository;

import java.util.Optional;

import com.microservice.authentication.common.model.Authentication;

import org.springframework.data.repository.Repository;

public interface AuthenticationCommonRepository extends Repository<Authentication, String> {
    Optional<Authentication> findByEmail(String email);
    Optional<Authentication> findById(String id);
    Authentication save(Authentication authentication);
}

