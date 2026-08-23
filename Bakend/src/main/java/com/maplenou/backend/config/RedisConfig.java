package com.maplenou.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Template String→String pour la blacklist JWT et le rate limiting.
     * Sérialiseurs String simples : pas de JSON, pas de type info.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer str = new StringRedisSerializer();
        template.setKeySerializer(str);
        template.setValueSerializer(str);
        template.setHashKeySerializer(str);
        template.setHashValueSerializer(str);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager Spring utilisé par @Cacheable / @CacheEvict.
     * Sérialise les valeurs en JSON (avec type info pour la désérialisation polymorphe).
     * TTL par cache :
     *   - categories     : 1 heure  (changement rare, admin seulement)
     *   - delivery-zones : 1 heure  (idem)
     *   - products       : 5 minutes (prix/stock peuvent changer)
     *   - shops          : 10 minutes
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> configs = Map.of(
                "categories",     defaults.entryTtl(Duration.ofHours(1)),
                "delivery-zones", defaults.entryTtl(Duration.ofHours(1)),
                "products",       defaults.entryTtl(Duration.ofMinutes(5)),
                "shops",          defaults.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(configs)
                .build();
    }
}
