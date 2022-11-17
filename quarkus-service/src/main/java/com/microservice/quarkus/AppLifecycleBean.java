package com.microservice.quarkus;

import com.microservice.quarkus.model.Company;
import com.mongodb.client.MongoClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.health.ServiceHealth;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class AppLifecycleBean {

	private static final Logger log = LoggerFactory.getLogger(AppLifecycleBean.class);

	@IfBuildProfile("consul")
	@ConfigProperty(name = "security.oauth2.resource.jwt.keyValue", defaultValue = "empty")
	String jwtValue;

	@ConfigProperty(name = "configuration.initialLoad", defaultValue = "true")
	boolean loadMockedData;

	@Inject
	MongoClient mongoClient;

	@IfBuildProfile("consul")
	@Inject
	Consul consulClient;

	@ConfigProperty(name = "quarkus.application.name")
	String appName;

	@ConfigProperty(name = "quarkus.application.version")
	String appVersion;

	@ConfigProperty(name = "HOSTNAME", defaultValue = "127.0.0.1")
	String hostname;

	@ConfigProperty(name = "QUARKUS_HTTP_PORT", defaultValue = "8080")
	int port;

	String instanceId = null;

	void onStart(@Observes StartupEvent ev) {
/*
		if (!StringUtils.equals(jwtValue, "empty")) {
			log.debug("Set Public Key: {}", jwtValue);
			log.debug("MongoDB settings: {}", mongoClient.getClusterDescription());
			File file = new File(AppLifecycleBean.class.getResource("/META-INF/resources").getFile(), "publicKey.pem");
			try {
				Files.write(file.toPath(), Collections.singletonList(jwtValue), StandardCharsets.UTF_8);
			} catch (IOException e) {
				log.error("IO write error", e);
				throw new RuntimeException(e);
			}
		}
*/
		if (loadMockedData) {
			if (Company.count() <= 0) {
				Company company = new Company();
				company.name = "Facebook";
				company.createdByUser = "default@admin.com";
				Company company1 = new Company();
				company1.name = "Google";
				company1.createdByUser = "default@admin.com";
				Company company2 = new Company();
				company2.name = "Amazon";
				company2.createdByUser = "default@admin.com";
				Company.persist(Stream.of(company, company1, company2));
			}
		}
		if (ProfileManager.getActiveProfile().contains("consul")) {
			HealthClient healthClient = consulClient.healthClient();
			List<ServiceHealth> instances = healthClient
					.getHealthyServiceInstances(appName).getResponse();
			instanceId = appName + "-" + instances.size();
			ImmutableRegistration registration = ImmutableRegistration.builder()
					.id(instanceId)
					.name(appName)
					.address(hostname)
					.port(port)
					.putMeta("version", appVersion)
					.build();
			consulClient.agentClient().register(registration);
			log.info("Instance registered: id={}", registration.getId());
		}
	}

	void onStop(@Observes ShutdownEvent ev) {
		if (ProfileManager.getActiveProfile().contains("consul") && consulClient != null && consulClient.agentClient().isRegistered(instanceId)) {
			consulClient.agentClient().deregister(instanceId);
			log.info("Instance de-registered: id={}", instanceId);
		}
	}
}