package com.ares.config;

import java.nio.file.Path;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves stored uploads and sets the multipart limits without changing the existing CORS setup. */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = Path.of("uploads").toAbsolutePath().normalize().toUri().toString();
        if (!uploadLocation.endsWith("/")) uploadLocation += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(uploadLocation);
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement("", DataSize.ofMegabytes(5).toBytes(),
                DataSize.ofMegabytes(6).toBytes(), 0);
    }
}
