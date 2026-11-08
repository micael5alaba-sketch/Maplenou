package com.maplenou.support.ticket.adapter;

import com.maplenou.support.platform.config.MaplenouProperties;
import com.maplenou.support.ticket.adapter.out.assistant.HttpAiAssistant;
import com.maplenou.support.ticket.adapter.out.assistant.NoAiAssistant;
import com.maplenou.support.ticket.application.TicketSettings;
import com.maplenou.support.ticket.application.port.AiAssistant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Wiring of the ticket module's settings and outbound adapters. */
@Configuration
public class TicketConfig {

    @Bean
    TicketSettings ticketSettings(MaplenouProperties props) {
        Duration after = props.tickets().autoCloseAfter();
        return new TicketSettings(after == null || after.isZero() ? null : after, props.cache().referenceTtl(), 10_000);
    }

    @Bean
    AiAssistant aiAssistant(MaplenouProperties props, RestClient.Builder builder) {
        MaplenouProperties.Assistant a = props.assistant();
        if (!a.configured()) {
            return new NoAiAssistant();
        }
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(5));
        rf.setReadTimeout(a.timeout());
        return new HttpAiAssistant(builder.clone().baseUrl(a.url()).requestFactory(rf)
                .defaultHeader("X-Assistant-Token", a.token()).build());
    }
}
