package ru.netology.cloudstorage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8080", "http://localhost:8081", "http://localhost:8082")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("auth-token")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
