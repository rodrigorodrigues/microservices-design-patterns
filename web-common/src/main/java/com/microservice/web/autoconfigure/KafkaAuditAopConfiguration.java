package com.microservice.web.autoconfigure;

import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Profile("kafka")
@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class KafkaAuditAopConfiguration {
    @Primary
    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        log.debug("AopConfiguration:producerFactory:kafkaProperties: {}", kafkaProperties);
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(new DefaultSslBundleRegistry()));
    }

    @Primary
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        log.debug("AopConfiguration:kafkaTemplate:producerFactory: {}", producerFactory);
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setObservationEnabled(true);
        return kafkaTemplate;
    }

    @Builder
    @Data
    static class ItemEvent {
        Object response;
        Integer statusCode;
        Date modifiedDate;
        String userModified;
        EventType eventType;

        enum EventType {
            CREATED,
            UPDATE,
            DELETED
        }
    }

    @AllArgsConstructor
    @EnableAsync
    @Component
    class AsyncEventListener {
        private final KafkaTemplate<String, Object> template;

        private final ObjectMapper objectMapper;

        private final WebConfigurationProperties webConfigurationProperties;

        @SneakyThrows
        @Async
        @EventListener
        public void itemEventListener(ItemEvent itemEvent) {
            log.debug("AsyncEventListener:itemEventListener:processing: {}", itemEvent);
            template.send(webConfigurationProperties.getKafkaTopic(), objectMapper.writeValueAsString(itemEvent));
            log.debug("AsyncEventListener:itemEventListener:processed:");
        }

    }

    @Component
    class ItemEventPublisher {

        private final ApplicationEventPublisher publisher;

        public ItemEventPublisher(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        public void publishItemEvent(ItemEvent itemEvent){
            publisher.publishEvent(itemEvent);
        }
    }

    @Aspect
    @AllArgsConstructor
    @Component
    class AopRestApiEvent {
        private final ItemEventPublisher itemEventPublisher;

        private final KafkaProperties kafkaProperties;

        @Pointcut("execution(* com.microservice.*.controller.*.*(..))")
        private void atServicePackage() {
        }

        @Pointcut("execution(* *.create(..))")
        private void isSaveMethod() {
        }

        @Pointcut("execution(* *.update(..))")
        private void isUpdateMethod() {
        }

        @Pointcut("execution(* *.delete(..))")
        private void isDeleteMethod() {
        }

        @AfterReturning(
                pointcut = "atServicePackage() && isSaveMethod()",
                returning = "result"
        )
        @SneakyThrows
        public void handleSave(ResponseEntity result) {
            if (result.getStatusCode().is2xxSuccessful()) {
                Object response = result.getBody();
                itemEventPublisher.publishItemEvent(ItemEvent.builder()
                        .eventType(ItemEvent.EventType.CREATED)
                        .statusCode(result.getStatusCode().value())
                        .modifiedDate(new Date())
                        .userModified(getAuthenticationUser())
                        .response(response)
                        .build());
                log.debug("aopRestApiEvent:kafkaProperties: {}", ToStringBuilder.reflectionToString(kafkaProperties));
                log.debug("aopRestApiEvent:handleSave: {}", response);
                log.debug("aopRestApiEvent:handleSave:headers: {}", result.getHeaders());
            } else {
                log.debug("Status is non ok skip kafka: {}", result.getStatusCode());
            }
        }

        @SneakyThrows
        @AfterReturning(
                pointcut = "atServicePackage() && isUpdateMethod()",
                returning = "result"
        )
        public void handleUpdate(JoinPoint joinPoint, ResponseEntity result) {
            if (result.getStatusCode().is2xxSuccessful()) {
                Object response = result.getBody();
                log.debug("aopRestApiEvent:handleUpdate: {}", response);
                log.debug("aopRestApiEvent:handleUpdate:headers: {}", result.getHeaders());
                itemEventPublisher.publishItemEvent(ItemEvent.builder()
                        .eventType(ItemEvent.EventType.UPDATE)
                        .statusCode(result.getStatusCode().value())
                        .modifiedDate(new Date())
                        .userModified(getAuthenticationUser())
                        .response(response)
                        .build());
            } else {
                log.debug("Status is non ok skip kafka: {}", result.getStatusCode());
            }
        }

        @SneakyThrows
        @After(value = "atServicePackage() && isDeleteMethod() && args(id,..)")
        public void handleDelete(String id) {
            log.debug("aopRestApiEvent:handleDelete: {}", id);
            itemEventPublisher.publishItemEvent(ItemEvent.builder()
                    .eventType(ItemEvent.EventType.DELETED)
                    .modifiedDate(new Date())
                    .userModified(getAuthenticationUser())
                    .response(id)
                    .build());
        }

        private String getAuthenticationUser() {
            Authentication principal = SecurityContextHolder.getContext().getAuthentication();
            return (principal != null && principal.isAuthenticated() ? principal.getName() : null);
        }

/*    @Around("atServicePackage() && isDeleteMethod()")
    public void handleDelete(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        Object[] args = proceedingJoinPoint.getArgs();
        List<Long> itemIdList = (List <Long>) args[0];

        try {
            proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            log.error(
                    "ItemNotificationAspect has detected the following error. No ItemEvent will be published due to the error. {}"
                    , throwable.getMessage()
            );
            throw throwable;
        }
    }*/
    }

}
