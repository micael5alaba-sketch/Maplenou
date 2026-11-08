package com.maplenou.support;

import com.maplenou.support.platform.config.MaplenouProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Maplenou customer service. Modules (bounded contexts) are packages: {@code ticket}, {@code sla},
 * {@code notification}, {@code attachment}, {@code context}, {@code identity}, {@code report}, plus the shared kernel
 * ({@code shared}), the technical platform ({@code platform}) and demo data ({@code demo}). Each module follows
 * ports and adapters: {@code domain} (pure Java) ← {@code application} (use cases + ports) ← {@code adapter.in}
 * / {@code adapter.out}. ArchitectureTest enforces these rules.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(MaplenouProperties.class)
public class MaplenouSupportApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaplenouSupportApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
