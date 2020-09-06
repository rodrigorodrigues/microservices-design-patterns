package com.microservice.kotlin.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.ApiKey
import springfox.documentation.service.AuthorizationScope
import springfox.documentation.service.SecurityReference
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.web.*
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*

@Configuration
@EnableSwagger2
@EnableWebMvc
class SwaggerConfig : WebMvcConfigurer {
    @Autowired
    lateinit var build: Optional<BuildProperties>
    @Autowired
    lateinit var git: Optional<GitProperties>
    @Bean
    fun api(): Docket {
        var version = "1.0"
        if (build.isPresent && git.isPresent) {
            var buildInfo = build.get()
            var gitInfo = git.get()
            version = "${buildInfo.version}-${gitInfo.shortCommitId}-${gitInfo.branch}"
        }
        return Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo(version))
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController::class.java))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(listOf(apiKey()))
                .securityContexts(Collections.singletonList(securityContext()))
                .useDefaultResponseMessages(false)
                .forCodeGeneration(true)
    }
    @Bean
    fun uiConfig(): UiConfiguration {
        return UiConfiguration(java.lang.Boolean.TRUE, java.lang.Boolean.FALSE, 1, 1, ModelRendering.MODEL, java.lang.Boolean.FALSE, DocExpansion.LIST, java.lang.Boolean.FALSE, null, OperationsSorter.ALPHA, java.lang.Boolean.FALSE, TagsSorter.ALPHA, UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS, null)
    }

    private fun apiInfo(version: String): ApiInfo {
        return ApiInfoBuilder()
                .title("API - Task Service")
                .description("Tasks Management")
                .version(version)
                .build()
    }

    private fun securityContext(): SecurityContext {
        return SecurityContext.builder().securityReferences(defaultAuth()).forPaths(PathSelectors.regex("/.*")).build()
    }

    private fun defaultAuth(): List<SecurityReference> {
        val authorizationScope = AuthorizationScope("global", "accessEverything")
        val authorizationScopes = arrayOf(authorizationScope)
        return listOf(SecurityReference("Bearer", authorizationScopes))
    }

    private fun apiKey(): ApiKey {
        return ApiKey("Bearer", "Authorization", "header")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/swagger-ui.html**")
            .addResourceLocations("classpath:/META-INF/resources/")

        registry.addResourceHandler("/v2/api-docs")
            .addResourceLocations("classpath:/META-INF/v2/api-docs")

        registry.addResourceHandler("/swagger-resources")
            .addResourceLocations("classpath:/META-INF/swagger-resources")

        registry.addResourceHandler("/swagger-resources/**")
            .addResourceLocations("classpath:/META-INF/swagger-resources/**")

        registry.addResourceHandler("/configuration/ui")
            .addResourceLocations("classpath:/META-INF/configuration/ui")

        registry.addResourceHandler("/configuration/security")
            .addResourceLocations("classpath:/META-INF/configuration/security")

        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")
    }


}
