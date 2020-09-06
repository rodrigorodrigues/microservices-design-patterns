package com.microservice.quarkus.dto;

import java.time.Instant;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.validation.constraints.NotBlank;

public class CompanyDto {
	@NotBlank
	private String name;
	private String createdByUser;
	private boolean activated;
	@JsonbDateFormat
	private Instant createdDate;
	private String lastModifiedByUser;
	@JsonbDateFormat
	private Instant lastModifiedDate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCreatedByUser() {
		return createdByUser;
	}

	public void setCreatedByUser(String createdByUser) {
		this.createdByUser = createdByUser;
	}

	public boolean isActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	public Instant getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Instant createdDate) {
		this.createdDate = createdDate;
	}

	public String getLastModifiedByUser() {
		return lastModifiedByUser;
	}

	public void setLastModifiedByUser(String lastModifiedByUser) {
		this.lastModifiedByUser = lastModifiedByUser;
	}

	public Instant getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Instant lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
}