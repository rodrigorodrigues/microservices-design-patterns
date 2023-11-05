package com.microservice.person;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import javax.crypto.spec.SecretKeySpec;

import brave.baggage.BaggageField;
import brave.baggage.CorrelationScopeConfig;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext;
import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import com.microservice.authentication.autoconfigure.AuthenticationProperties;
import com.microservice.person.config.ConfigProperties;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.model.Person;
import com.microservice.person.repository.PersonRepository;
import com.microservice.person.service.PersonService;
import com.microservice.web.common.util.ChangeQueryStringFilter;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.querydsl.binding.PathInformation;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilderCustomizer;
import org.springframework.data.util.TypeInformation;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(ConfigProperties.class)
public class PersonServiceApplication implements ApplicationContextAware {
    Faker faker = new Faker();
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
		SpringApplication.run(PersonServiceApplication.class, args);
	}

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean(name = "requestId")
    public BaggageField requestId() {
        return BaggageField.create("requestId");
    }

    @Bean
    public CurrentTraceContext.ScopeDecorator mdcScopeDecorator(@Qualifier("requestId") BaggageField requestId) {
        return MDCScopeDecorator.newBuilder().clear()
            .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(requestId).flushOnUpdate().build())
            .build();
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Primary
    @Profile("prod")
    @Bean
    RSAPublicKey publicKeyStore(@Value("${com.microservice.authentication.jwt.publicKeyStore}") RSAPublicKey key) {
        return key;
    }

    @Primary
    @Bean
    public JwtDecoder jwtDecoder(AuthenticationProperties properties) {
        log.debug("jwtDecoder:properties: {}", properties);
        AuthenticationProperties.Jwt jwt = properties.getJwt();
        if (jwt != null && jwt.getKeyValue() != null) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwt.getKeyValue().getBytes(StandardCharsets.UTF_8), "HS256");
            return NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
        } else {
            RSAPublicKey publicKey = applicationContext.getBean(RSAPublicKey.class);
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /*@Configuration
    @ConditionalOnProperty(prefix = "kafka.local.test", name = "enabled", havingValue = "true")
    @AllArgsConstructor
	class KafkaConsumerConfiguration {
        private final KafkaTemplate<String, String> template;

        @Bean
        public NewTopic topic3() {
            return TopicBuilder.name("topic3")
                .partitions(1)
                .replicas(1)
                .build();
        }

        @Bean
        public NewTopic topic2() {
            return TopicBuilder.name("topic2")
                .partitions(1)
                .replicas(1)
                .build();
        }

        @KafkaListener(id = "myId", topics = "topic3")
        public void listenTopic1(String in) {
            log.info("Kafka Message receiving:topic3 {}", in);
        }

        @KafkaListener(id = "myId2", topics = "topic2")
        public void listenTopic2(String in) {
            log.info("Kafka Message receiving:topic2 {}", in);
        }
    }

    @Configuration
    @ConditionalOnProperty(prefix = "producer", name = "enabled", havingValue = "true")
    @AllArgsConstructor
    class KafkaProducerConfiguration {
        private final KafkaTemplate<String, String> template;
*/
  //      @Scheduled(cron = "*/1 * * * * *")
    /*    public void producer() {
            Space space = faker.space();
            Name name = faker.name();
            log.info("Sending Kafka message for topic 3");
            template.send("topic3", name.fullName());
            log.info("Sending Kafka message for topic 2");
            template.send("topic2", space.planet());
        }
    }*/

    @Bean
    @LoadBalanced
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    BuildProperties buildProperties() {
        return new BuildProperties(new Properties());
    }

    @ConditionalOnProperty(prefix = "load.data", name = "people", havingValue = "true")
    @Bean
    CommandLineRunner runner(PersonService personService, @Value("${load.data.people.total:40}") Integer total, PersonRepository personRepository) {
        return args -> {
            if (personRepository.count() == 0) {
                Address address = faker.address();
                IntStream.range(0, total).forEach(i -> {
                    PersonDto personDto = PersonDto.builder()
                        .fullName(faker.name().fullName())
                        .address(new PersonDto.Address(null, address.fullAddress(), address.city(), address.state(), address.country(), address.zipCode()))
                        .dateOfBirth(faker.date().birthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                        .build();
                    log.info("personDto: {}", personService.save(personDto));
                });
            }
        };
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

    @ConditionalOnMissingBean
    @Bean
    GitProperties gitProperties() {
        return new GitProperties(new Properties());
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ChangeQueryStringFilter<Person> changeQueryStringFilter() {
        return new ChangeQueryStringFilter<>() {
            @Override
            public Class<Person> getObjectType() {
                return Person.class;
            }

            @Override
            public Class<? extends Annotation> getEntityType() {
                return Document.class;
            }
        };
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
