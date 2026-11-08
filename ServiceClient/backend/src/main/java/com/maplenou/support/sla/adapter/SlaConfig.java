package com.maplenou.support.sla.adapter;

import com.maplenou.support.platform.config.MaplenouProperties;
import com.maplenou.support.sla.application.SlaSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class SlaConfig {

    @Bean
    SlaSettings slaSettings(MaplenouProperties props) {
        MaplenouProperties.Sla s = props.sla();
        return new SlaSettings(ZoneId.of(s.zone()), s.opening(), s.closing(), s.continuous(),
                props.cache().referenceTtl());
    }
}
