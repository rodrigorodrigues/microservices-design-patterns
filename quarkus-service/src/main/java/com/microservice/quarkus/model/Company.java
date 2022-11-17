package com.microservice.quarkus.model;

import java.time.Instant;

import javax.validation.constraints.NotBlank;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.panache.common.Page;

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

	private static final String query = "activated = ?1 and createdByUser = ?2";

	public static CompanyHelper findActiveCompanies(Page page) {
		return new CompanyHelper(Company.find("activated", true)
				.page(page).list(), count("activated", true));
	}

	public static CompanyHelper findActiveCompaniesByUser(Page page, String user) {
		return new CompanyHelper(Company.find(query, true, user)
				.page(page).list(), count(query, true, user));
	}
}
