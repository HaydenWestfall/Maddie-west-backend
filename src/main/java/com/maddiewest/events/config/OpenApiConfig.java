package com.maddiewest.rentalservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI maddieWestEventsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Maddie West Events API")
                        .description("Comprehensive API documentation for the Maddie West Events backend: rental inventory, availability, request approval workflow, and the website contact form.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Maddie West Events")
                                .email("support@maddiewestevents.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://maddiewestevents.com")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
