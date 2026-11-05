package com.ares.config;

import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/** Enables response gzip without editing the existing application properties. */
@Configuration
public class ResponseCompressionConfig {
    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> responseCompression() {
        return factory -> {
            Compression compression = new Compression();
            compression.setEnabled(true);
            compression.setMinResponseSize(DataSize.ofKilobytes(1));
            compression.setMimeTypes(new String[]{"application/json", "application/xml", "text/xml",
                    "text/html", "text/plain", "text/css", "application/javascript", "image/svg+xml"});
            factory.setCompression(compression);
        };
    }
}
