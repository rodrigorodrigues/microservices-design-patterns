package com.microservice.authentication;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableRedisWebSession
public class AuthenticationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthenticationServiceApplication.class, args);
	}

	@Bean
	public ValidatingMongoEventListener validatingMongoEventListener() {
		return new ValidatingMongoEventListener(validator());
	}

	@Bean
	public LocalValidatorFactoryBean validator() {
		return new LocalValidatorFactoryBean();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

    @Bean
    RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
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
}
