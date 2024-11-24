package com.microservice.authentication.repository;

import com.microservice.authentication.model.WebauthnRegistration;

import org.springframework.data.repository.CrudRepository;

public interface WebauthnRegistrationRepository extends CrudRepository<WebauthnRegistration, String> {
}
