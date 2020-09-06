package com.springboot.bootstrap;

import com.netflix.discovery.DefaultEurekaClientConfig;
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
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Profile("prod-need-to-fix")
@Configuration
@BootstrapConfiguration
public class SslClientAutoConfiguration {
    @Primary
    @Bean
    public SSLContext sslContext(@Value("${service.security.trustStorePath:/etc/ssl/truststore.jks}") String trustStorePath,
                                 @Value("${service.security.trustStorePassword:changeit}") String trustStorePassword,
                                 @Value("${server.ssl.key-store}") String keystorePath,
                                 @Value("${server.ssl.key-store-password}") String keystorePassword) throws Exception {
        return new SSLContextBuilder()
                .loadTrustMaterial(new File(trustStorePath), trustStorePassword.toCharArray())
                .loadKeyMaterial(new File(keystorePath), keystorePassword.toCharArray(), keystorePassword.toCharArray())
                .build();
    }

    @Bean
    public BeanFactoryPostProcessor registerPostProcessor() {
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

    @Primary
    @Bean
    public DiscoveryClient.DiscoveryClientOptionalArgs discoveryClientOptionalArgs(@Value("${server.port:8443}") Integer serverPort,
                                                                                  SSLContext sslContext,
                                                                                  SSLConnectionSocketFactory systemSocketFactory) {
        SchemeRegistry sslSchemeRegistry = new SchemeRegistry();
        Scheme schema = new Scheme("https", serverPort, new SSLSocketFactoryAdapter(systemSocketFactory));
        sslSchemeRegistry.register(schema);
        String name = "Custom-Discovery-Client";
        MonitoredConnectionManager connectionManager = new MonitoredConnectionManager(name, sslSchemeRegistry);
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getProperties().put(ApacheHttpClient4Config.PROPERTY_CONNECTION_MANAGER, connectionManager);

        EurekaClientConfig config = new DefaultEurekaClientConfig();

        DiscoveryJerseyProvider discoveryJerseyProvider = new DiscoveryJerseyProvider(
                CodecWrappers.getEncoder(config.getEncoderName()),
                CodecWrappers.resolveDecoder(config.getDecoderName(), config.getClientDataAccept()));

        clientConfig.getSingletons().add(discoveryJerseyProvider);

        DiscoveryClient.DiscoveryClientOptionalArgs clientOptionalArgs = new DiscoveryClient.DiscoveryClientOptionalArgs();
        clientOptionalArgs.setSSLContext(sslContext);
        clientOptionalArgs.setHostnameVerifier(new NoopHostnameVerifier());
        clientOptionalArgs.setEurekaJerseyClient(new EurekaJerseyClientImpl(
                config.getEurekaServerConnectTimeoutSeconds() * 1000,
                config.getEurekaServerReadTimeoutSeconds() * 1000,
                config.getEurekaConnectionIdleTimeoutSeconds() * 1000,
                clientConfig));
        return clientOptionalArgs;
    }

    @Primary
    @Bean
    public SSLConnectionSocketFactory getSslConnectionSocketFactory(@Value("${service.security.trustStorePath:/etc/ssl/truststore.jks}") String trustStorePath,
                                                                     @Value("${service.security.trustStorePassword:changeit}") String trustStorePassword,
                                                                     @Value("${service.security.trustStoreType:JKS}") String trustStoreType) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        final KeyStore trustStore = KeyStore.getInstance(trustStoreType);
        trustStore.load(new FileSystemResource(trustStorePath).getInputStream(), trustStorePassword.toCharArray());

        return new SSLConnectionSocketFactory(
                SSLContexts
                        .custom()
                        .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                        .build(),
                new NoopHostnameVerifier());
    }

}
