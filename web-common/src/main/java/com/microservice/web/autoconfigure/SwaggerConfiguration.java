package com.microservice.web.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.AllArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger Configuration
 */
@ConditionalOnProperty(prefix = "configuration", name = "swagger", havingValue = "true", matchIfMissing = true)
@Configuration
@AllArgsConstructor
public class SwaggerConfiguration {

    private final BuildProperties build;

    private final GitProperties git;

    @Bean
    public OpenAPI springShopOpenAPI() {
        String version = String.format("%s-%s-%s", build.getVersion(), git.getShortCommitId(), git.getBranch());
        SecurityScheme securitySchemesItem = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .info(new Info().title("Authentication REST API")
                        .description("Spring shop sample application")
                        .version(version)
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .externalDocs(new ExternalDocumentation()
                        .description("SpringShop Wiki Documentation")
                        .url("https://github.com/rodrigorodrigues/microservices-design-patterns"))
                .components(new Components().addSecuritySchemes("bearer-key", securitySchemesItem));
    }
/*

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(Arrays.asList(apiKey()))
                .securityContexts(Collections.singletonList(securityContext()))
                .apiInfo(apiEndPointsInfo());
    }

    private ApiInfo apiEndPointsInfo() {
        String version = String.format("%s-%s-%s", build.getVersion(), git.getShortCommitId(), git.getBranch());
        return new ApiInfoBuilder()
                .description("REST API")
                .contact(new Contact("Rodrigo Santos", "https://github.com/rodrigorodrigues/microservices-design-patterns", "rodrigorodriguesweb@gmail.com"))
                .license("Apache 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html")
                .version(version)
                .build();
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth()).forPaths(PathSelectors.regex("/.*")).build();
    }

    private List<SecurityReference> defaultAuth() {
        final AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        final AuthorizationScope[] authorizationScopes = new AuthorizationScope[]{authorizationScope};
        return Collections.singletonList(new SecurityReference("Bearer", authorizationScopes));
    }

    private ApiKey apiKey() {
        return new ApiKey("Bearer", "Authorization", "header");
    }
*/

}
