package com.telecom.insights.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Telecom Insights API")
                        .version("1.0.0")
                        .description("API documentation for Telecom Insights Backend")
                        .contact(new Contact()
                                .name("Telecom Insights Team")
                                .url("https://telecominsights.com")));
    }



}

