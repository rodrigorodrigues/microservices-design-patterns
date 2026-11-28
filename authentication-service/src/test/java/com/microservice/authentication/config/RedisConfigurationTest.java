package com.microservice.authentication.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigurationTest {

    @Test
    @SuppressWarnings("unchecked")
    void sessionsUseTheConfiguredIdleTimeoutInsteadOfThirtyMinutes() {
        RedisSessionRepository sessionRepository = new RedisSessionRepository(mock(RedisOperations.class));
        Session before = sessionRepository.createSession();
        assertThat(before.getMaxInactiveInterval())
            .isEqualTo(Duration.ofSeconds(MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS));

        new RedisConfiguration().sessionTimeoutCustomizer(Duration.ofHours(10)).customize(sessionRepository);

        Session after = sessionRepository.createSession();
        assertThat(after.getMaxInactiveInterval()).isEqualTo(Duration.ofHours(10));
    }
}
