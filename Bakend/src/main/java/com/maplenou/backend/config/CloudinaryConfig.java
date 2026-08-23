package com.maplenou.backend.config;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CloudinaryConfig {

    @Value("${app.cloudinary.url}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        if (cloudinaryUrl.contains("dev-key") || cloudinaryUrl.contains("dev-cloud")) {
            log.warn("Cloudinary non configuré (clés de développement placeholder) — " +
                    "définir CLOUDINARY_URL avec les vraies clés avant utilisation en production.");
        }
        return new Cloudinary(cloudinaryUrl);
    }
}
