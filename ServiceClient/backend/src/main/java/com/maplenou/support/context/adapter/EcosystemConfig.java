package com.maplenou.support.context.adapter;

import com.maplenou.support.context.adapter.out.maplenou.HttpEcosystemActions;
import com.maplenou.support.context.adapter.out.maplenou.HttpEcosystemGateway;
import com.maplenou.support.context.adapter.out.maplenou.HttpLegacyTicketSource;
import com.maplenou.support.context.application.port.EcosystemActions;
import com.maplenou.support.context.application.port.EcosystemGateway;
import com.maplenou.support.context.application.port.LegacyTicketSource;
import com.maplenou.support.platform.config.MaplenouProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Adapters to Maplenou (Bakend) when {@code ECOSYSTEM_MODE=http}. With {@code demo} (default), the demo module
 * provides sample data instead.
 */
@Configuration
public class EcosystemConfig {

    /** Header checked by Bakend on /internal/v1/**. */
    public static final String SERVICE_TOKEN_HEADER = "X-Maplenou-Service-Token";

    @Bean
    LegacyTicketSource legacyTicketSource(MaplenouProperties props, RestClient.Builder builder) {
        return new HttpLegacyTicketSource(props, builder);
    }

    @Bean
    @ConditionalOnProperty(prefix = "maplenou.ecosystem", name = "mode", havingValue = "http")
    EcosystemGateway httpEcosystemGateway(MaplenouProperties props, RestClient.Builder builder) {
        return new HttpEcosystemGateway(client(props, builder));
    }

    @Bean
    @ConditionalOnProperty(prefix = "maplenou.ecosystem", name = "mode", havingValue = "http")
    EcosystemActions httpEcosystemActions(MaplenouProperties props, RestClient.Builder builder) {
        return new HttpEcosystemActions(client(props, builder));
    }

    static RestClient client(MaplenouProperties props, RestClient.Builder builder) {
        MaplenouProperties.Ecosystem cfg = props.ecosystem();
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(cfg.timeout());
        rf.setReadTimeout(cfg.timeout());
        RestClient.Builder b = builder.clone().baseUrl(cfg.baseUrl()).requestFactory(rf);
        if (cfg.serviceToken() != null && !cfg.serviceToken().isBlank()) {
            // Bakend: supportbridge.SupportBridgeTokenFilter (no user JWT on this service API)
            b.defaultHeader(SERVICE_TOKEN_HEADER, cfg.serviceToken());
        }
        return b.build();
    }
}
