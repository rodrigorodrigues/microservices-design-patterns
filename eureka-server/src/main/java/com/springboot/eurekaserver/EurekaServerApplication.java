package com.springboot.eurekaserver;

import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.converters.wrappers.CodecWrappers;
import com.netflix.discovery.provider.DiscoveryJerseyProvider;
import com.netflix.discovery.shared.MonitoredConnectionManager;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;
import com.netflix.discovery.shared.transport.jersey.SSLSocketFactoryAdapter;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.security.KeyStore;

@Slf4j
@SpringBootApplication
@EnableRedisHttpSession
@EnableEurekaServer
public class EurekaServerApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }

    @Bean
    public LettuceConnectionFactory connectionFactory(RedisProperties redisProperties) {
        return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
    }

    @Profile("prod")
    @Configuration
    class SslNoopHostnameVerifierConfiguration {

        @Value("${service.security.trustStorePath:/etc/ssl/truststore.jks}")
        private String trustStorePath;

        @Value("${service.security.trustStorePassword:changeit}")
        private String trustStorePassword;

        @Value("${service.security.trustStoreType:JKS}")
        private String trustStoreType;

        @Autowired
        private EurekaClientConfig config;

        @Bean
        public DiscoveryClient.DiscoveryClientOptionalArgs getTrustStoredEurekaClient()
            throws Exception {
            final KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            trustStore.load(new FileSystemResource(trustStorePath).getInputStream(), trustStorePassword.toCharArray());

            SSLConnectionSocketFactory systemSocketFactory = new SSLConnectionSocketFactory(
                SSLContexts
                    .custom()
                    .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                    .build(),
                new NoopHostnameVerifier());

            SchemeRegistry sslSchemeRegistry = new SchemeRegistry();
            Scheme schema = new Scheme("https", 8443, new SSLSocketFactoryAdapter(systemSocketFactory));
            sslSchemeRegistry.register(schema);
            String name = "Custom-Discovery-Client";
            MonitoredConnectionManager connectionManager = new MonitoredConnectionManager(name, sslSchemeRegistry);
            ClientConfig clientConfig = new DefaultClientConfig();
            clientConfig.getProperties().put(ApacheHttpClient4Config.PROPERTY_CONNECTION_MANAGER, connectionManager);

            DiscoveryJerseyProvider discoveryJerseyProvider = new DiscoveryJerseyProvider(
                CodecWrappers.getEncoder(config.getEncoderName()),
                CodecWrappers.resolveDecoder(config.getDecoderName(), config.getClientDataAccept()));

            clientConfig.getSingletons().add(discoveryJerseyProvider);

            DiscoveryClient.DiscoveryClientOptionalArgs clientOptionalArgs = new DiscoveryClient.DiscoveryClientOptionalArgs();
            clientOptionalArgs.setEurekaJerseyClient(new EurekaJerseyClientImpl(
                config.getEurekaServerConnectTimeoutSeconds() * 1000,
                config.getEurekaServerReadTimeoutSeconds() * 1000,
                config.getEurekaConnectionIdleTimeoutSeconds() * 1000,
                clientConfig));
            return clientOptionalArgs;
        }
    }

}

