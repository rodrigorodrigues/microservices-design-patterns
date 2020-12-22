package com.microservice.quarkus;

import com.microservice.quarkus.model.Company;
import com.mongodb.client.MongoClient;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.stream.Stream;

@ApplicationScoped
public class AppLifecycleBean {

	private static final Logger log = LoggerFactory.getLogger(AppLifecycleBean.class);

	@ConfigProperty(name = "security.oauth2.resource.jwt.keyValue")
	String jwtValue;

	@ConfigProperty(name = "configuration.initialLoad", defaultValue = "true")
	boolean loadMockedData;

	@Inject
	MongoClient mongoClient;

	void onStart(@Observes StartupEvent ev) {
		log.debug("Set Public Key: {}", jwtValue);
		log.debug("MongoDB settings: {}", mongoClient.getClusterDescription());
		File file = new File(AppLifecycleBean.class.getResource("/META-INF/resources").getFile(), "publicKey.pem");
		try {
			Files.write(file.toPath(), Collections.singletonList(jwtValue), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("IO write error", e);
			throw new RuntimeException(e);
		}
		if (loadMockedData) {
			Company.count()
					.subscribe().with(i -> {
						log.debug("count: {}", i);
						if (i == 0) {
							Company company = new Company();
							company.name = "Facebook";
							company.createdByUser = "default@admin.com";
							Company company1 = new Company();
							company1.name = "Google";
							company1.createdByUser = "default@admin.com";
							Company company2 = new Company();
							company2.name = "Amazon";
							company2.createdByUser = "default@admin.com";
							Company.persist(Stream.of(company, company1, company2))
									.await()
									.indefinitely();
						}
					}, RuntimeException::new);
		}
	}

}