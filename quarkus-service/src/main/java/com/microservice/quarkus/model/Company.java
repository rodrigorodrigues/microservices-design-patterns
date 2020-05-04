package com.microservice.quarkus.model;

import java.time.Instant;
import java.util.List;

import javax.validation.constraints.NotBlank;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

@MongoEntity(collection = "company")
public class Company extends PanacheMongoEntity {
	@NotBlank
	public String name;
	@NotBlank
	public String createdByUser;
	public boolean activated = true;
	public Instant createdDate = Instant.now();
	public String lastModifiedByUser;
	public Instant lastModifiedDate = Instant.now();

	public static List<Company> findActiveCompanies() {
		return list("activated", true);
	}

	public static List<Company> findActiveCompaniesByUser(String user) {
		return list("activated = ?1 and createdByUser = ?2", true, user);
	}
}
