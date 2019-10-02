package com.springboot.configserver;

import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.converters.wrappers.CodecWrappers;
import com.netflix.discovery.provider.DiscoveryJerseyProvider;
import com.netflix.discovery.shared.MonitoredConnectionManager;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;
import com.netflix.discovery.shared.transport.jersey.SSLSocketFactoryAdapter;
import com.springboot.configserver.config.ConfigServerProperties;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@SpringBootApplication(scanBasePackages = {"com.learning.autoconfigure", "com.springboot.configserver"})
@EnableConfigServer
@EnableDiscoveryClient
@EnableConfigurationProperties(ConfigServerProperties.class)
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

    @Profile("prod")
    @Primary
    @Bean
    public static BeanFactoryPostProcessor registerPostProcessor() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
                if (beanDefinitionName.equalsIgnoreCase("discoveryClientOptionalArgs")) {
                    BeanDefinition beanDefinition = registry.containsBeanDefinition(beanDefinitionName) ? registry.getBeanDefinition(beanDefinitionName) : null;
                    if (beanDefinition != null) {
                        if (registry.containsBeanDefinition(beanDefinitionName)) {
                            registry.removeBeanDefinition(beanDefinitionName);
                        }
                    }
                }
            }
        };
    }

    @Profile("prod")
    @Configuration
    class SslConfiguration {
        @Value("${service.security.trustStorePath:/etc/ssl/truststore.jks}")
        private String trustStorePath;

        @Value("${service.security.trustStorePassword:changeit}")
        private String trustStorePassword;

        @Value("${service.security.trustStoreType:JKS}")
        private String trustStoreType;

        @Value("${server.port:8443}")
        private Integer serverPort;

        @Autowired
        private EurekaClientConfig config;

        @Bean
        public DiscoveryClient.DiscoveryClientOptionalArgs getTrustStoredEurekaClient()
            throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, KeyManagementException {
            final KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            trustStore.load(new FileSystemResource(trustStorePath).getInputStream(), trustStorePassword.toCharArray());

            SSLConnectionSocketFactory systemSocketFactory = new SSLConnectionSocketFactory(
                SSLContexts
                    .custom()
                    .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                    .build(),
                new NoopHostnameVerifier());

            SchemeRegistry sslSchemeRegistry = new SchemeRegistry();
            Scheme schema = new Scheme("https", serverPort, new SSLSocketFactoryAdapter(systemSocketFactory));
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
            clientOptionalArgs.setHostnameVerifier(new AllowAllHostnameVerifier());
            clientOptionalArgs.setEurekaJerseyClient(new EurekaJerseyClientImpl(
                config.getEurekaServerConnectTimeoutSeconds() * 1000,
                config.getEurekaServerReadTimeoutSeconds() * 1000,
                config.getEurekaConnectionIdleTimeoutSeconds() * 1000,
                clientConfig));
            return clientOptionalArgs;
        }
    }
}

