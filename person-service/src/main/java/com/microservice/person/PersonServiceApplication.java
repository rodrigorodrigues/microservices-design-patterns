package com.microservice.person;

import java.time.ZoneId;
import java.util.Properties;
import java.util.stream.IntStream;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import com.github.javafaker.Space;
import com.microservice.person.config.ConfigProperties;
import com.microservice.person.dto.PersonDto;
import com.microservice.person.repository.PersonRepository;
import com.microservice.person.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ConfigProperties.class)
public class PersonServiceApplication implements EnvironmentAware, WebMvcConfigurer {
    Faker faker = new Faker();

    public static void main(String[] args) {
		SpringApplication.run(PersonServiceApplication.class, args);
	}

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/swagger/swagger-ui.html", "/swagger-ui.html");
    }

	@Configuration
    @ConditionalOnProperty(prefix = "kafka.local.test", name = "enabled", havingValue = "true")
	class KafkaTestConfiguration {
        @Autowired
        KafkaTemplate<String, String> template;

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

        @ConditionalOnProperty(prefix = "producer", name = "enabled", havingValue = "true")
        @Scheduled(cron = "*/1 * * * * *")
        public void producer() {
            Space space = faker.space();
            Name name = faker.name();
            log.info("Sending Kafka message for topic 3");
            template.send("topic3", name.fullName());
            log.info("Sending Kafka message for topic 2");
            template.send("topic2", space.planet());
        }
    }

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

    @Override
    public void setEnvironment(Environment environment) {
    }
}
