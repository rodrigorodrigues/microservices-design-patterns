package com.microservice.quarkus;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;

public class MongoTestResource implements QuarkusTestResourceLifecycleManager {
	private static MongodExecutable MONGO;

	private static final Logger LOGGER = Logger.getLogger(MongoTestResource.class);

	@Override
	public Map<String, String> start() {
		try {
//			System.setProperty("os.arch", "i686_64");
			Version.Main version = Version.Main.DEVELOPMENT;
			MongodConfig config = MongodConfig.builder()
					.version(version)
					.net(new Net())
					.build();
			MONGO = MongodStarter.getDefaultInstance().prepare(config);
			MongodProcess mongodProcess = MONGO.start();
			int port = mongodProcess.getConfig().net().getPort();
			LOGGER.infof("Started Embedded Mongo %s on port %s", version, port);

			return Collections.singletonMap("%test.quarkus.mongodb.connection-string", String.format("mongodb://localhost:%s", port));
		} catch (IOException e) {
			LOGGER.error("Could not start embedded mongodb", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void stop() {
		if (MONGO != null) {
			MONGO.stop();
		}
	}
}