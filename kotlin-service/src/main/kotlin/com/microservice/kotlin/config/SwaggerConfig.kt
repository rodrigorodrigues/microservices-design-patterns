package com.microservice.kotlin.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.web.*
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*

@Configuration
@EnableSwagger2
class SwaggerConfig {
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
                .apis(RequestHandlerSelectors.any())
                .paths{ it.equals("/api/tasks")}
                .build()
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
}
