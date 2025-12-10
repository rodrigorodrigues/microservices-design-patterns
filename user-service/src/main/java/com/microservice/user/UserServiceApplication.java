package com.microservice.user;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.querydsl.binding.PathInformation;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilderCustomizer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Primary
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    BuildProperties buildProperties() {
        return new BuildProperties(new Properties());
    }

    @ConditionalOnMissingBean
    @Bean
    GitProperties gitProperties() {
        return new GitProperties(new Properties());
    }

    @Primary
    @Profile("prod")
    @Bean
    RSAPublicKey publicKeyStore(@Value("${com.microservice.authentication.jwt.publicKeyStore}") RSAPublicKey key) {
        return key;
    }

    @ConditionalOnMissingBean
    @Bean
    QuerydslPredicateBuilderCustomizer querydslPredicateBuilderCustomizer(QuerydslBindingsFactory querydslBindingsFactory) {
        return new QuerydslPredicateBuilder(DefaultConversionService.getSharedInstance(), querydslBindingsFactory.getEntityPathResolver()) {
            @Override
            public Predicate getPredicate(TypeInformation<?> type, MultiValueMap<String, ?> values, QuerydslBindings bindings) {
                Assert.notNull(bindings, "Context must not be null");

                BooleanBuilder builder = new BooleanBuilder();

                if (values.isEmpty()) {
                    return getPredicate(builder);
                }

                for (Map.Entry<String, ? extends List<?>> entry : values.entrySet()) {

                    if (isSingleElementCollectionWithEmptyItem(entry.getValue())) {
                        continue;
                    }

                    String path = entry.getKey();

                    if (!bindings.isPathAvailable(path, type)) {
                        continue;
                    }

                    PathInformation propertyPath = bindings.getPropertyPath(path, type);

                    if (propertyPath == null) {
                        continue;
                    }

                    Collection<Object> value = convertToPropertyPathSpecificType(entry.getValue(), propertyPath, conversionService);
                    Optional<Predicate> predicate = invokeBinding(propertyPath, bindings, value, resolver, defaultBinding);

                    predicate.ifPresent(builder::or);
                }

                return getPredicate(builder);
            }
        };
    }
}
