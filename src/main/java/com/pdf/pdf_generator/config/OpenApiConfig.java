package com.pdf.pdf_generator.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pdfGeneratorOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("PDF Generator API")
                        .description("APIs for generating PDFs from XML")
                        .version("v2.0"));
    }
}
