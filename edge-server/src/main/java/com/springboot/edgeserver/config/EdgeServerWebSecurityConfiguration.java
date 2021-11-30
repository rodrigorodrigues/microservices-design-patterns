package com.springboot.edgeserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.jackson2.SecurityJackson2Modules;

@Configuration
@EnableWebSecurity
public class EdgeServerWebSecurityConfiguration extends WebSecurityConfigurerAdapter implements BeanClassLoaderAware {

    private static final String[] WHITELIST = {
        "/api/**",
        "/oauth2/**",
        "/.well-known/jwks.json",
        "/swagger/**",
        "/**/*.js",
        "/**/*.css",
        "/**/*.html",
        "/favicon.ico",
        "/actuator/**",
        "/error"
    };

    private ClassLoader loader;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest().hasRole("ADMIN")
                .and()
                .formLogin()
                .and()
                .logout();
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(WHITELIST);
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    /**
     * Customized {@link ObjectMapper} to add mix-in for class that doesn't have default
     * constructors
     *
     * @return the {@link ObjectMapper} to use
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
        return mapper;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }
}
