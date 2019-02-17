package com.learning.springboot.repository;

import com.learning.springboot.model.Authentication;
import org.springframework.data.repository.Repository;

public interface AuthenticationRepository extends Repository<Authentication, String> {
    Authentication findByEmail(String email);
}
