package com.maplenou.support.demo;

import com.maplenou.support.context.application.port.EcosystemActions;
import com.maplenou.support.context.application.port.EcosystemGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Sample Maplenou data when {@code ECOSYSTEM_MODE=demo} (default): the whole workflow runs without Bakend. */
@Configuration
@ConditionalOnProperty(prefix = "maplenou.ecosystem", name = "mode", havingValue = "demo", matchIfMissing = true)
public class DemoEcosystemConfig {

    @Bean
    EcosystemGateway demoEcosystemGateway() {
        return new DemoEcosystemGateway();
    }

    @Bean
    EcosystemActions demoEcosystemActions() {
        return new DemoEcosystemActions();
    }
}
