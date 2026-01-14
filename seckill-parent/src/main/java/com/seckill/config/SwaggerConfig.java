package com.seckill.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("秒杀系统API")
                        .description("在线秒杀系统后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("开发者")
                                .email("admin@example.com")
                                .url("http://localhost:8080"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("Spring Boot Wiki Documentation")
                        .url("https://spring.io/projects/spring-boot"));
    }
}