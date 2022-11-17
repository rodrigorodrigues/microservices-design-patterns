package com.microservice.quarkus.model;

import java.util.List;

public record CompanyHelper(List<Company> companies, long count) {
}
