package com.microservice.quarkus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.microservice.quarkus.model.Company;
import com.mongodb.client.MongoClient;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AppLifecycleBean {

	private static final Logger log = LoggerFactory.getLogger(AppLifecycleBean.class);

	@ConfigProperty(name = "security.oauth2.resource.jwt.keyValue")
	String jwtValue;

	@ConfigProperty(name = "configuration.initialLoad", defaultValue = "true")
	boolean loadMockedData;

	@Inject
	MongoClient mongoClient;

	void onStart(@Observes StartupEvent ev) throws IOException {
		log.info("Set Public Key: {}", jwtValue);
		log.info("MongoDB settings: {}", mongoClient.getClusterDescription());
		File file = new File(AppLifecycleBean.class.getResource("/META-INF/resources").getFile(), "publicKey.pem");
		Files.write(file.toPath(), Collections.singletonList(jwtValue), StandardCharsets.UTF_8);
		if (loadMockedData && Company.count() == 0) {
			Company company = new Company();
			company.name = "Facebook";
			company.createdByUser = "default@admin.com";
			Company company1 = new Company();
			company1.name = "Google";
			company1.createdByUser = "default@admin.com";
			Company company2 = new Company();
			company2.name = "Amazon";
			company2.createdByUser = "default@admin.com";
			Stream.of(company, company1, company2)
					.forEach(c -> c.persistOrUpdate());
		}
	}

}