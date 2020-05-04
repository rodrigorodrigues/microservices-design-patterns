package com.microservice.quarkus.dto;

import javax.validation.constraints.NotBlank;

public class CompanyDto {
	@NotBlank
	private String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
