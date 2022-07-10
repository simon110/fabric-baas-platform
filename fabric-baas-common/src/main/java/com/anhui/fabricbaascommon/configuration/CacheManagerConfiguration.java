package com.anhui.fabricbaascommon.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

@Configuration
public class CacheManagerConfiguration {
    @Value("#{'${spring.cache.redis.cache-names}'.split(',')}")
    private String[] cacheNames;

    @SuppressWarnings({"deprecation", "SpringJavaInjectionPointsAutowiringInspection"})
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisSerializationContext.SerializationPair<String> keySerializer = fromSerializer(new StringRedisSerializer());
        RedisSerializationContext.SerializationPair<Object> valueSerializer = fromSerializer(new GenericJackson2JsonRedisSerializer());
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory);

        for (String cacheName : cacheNames) {
            RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .prefixKeysWith(cacheName + ':')
                    .disableCachingNullValues()
                    .serializeKeysWith(keySerializer)
                    .serializeValuesWith(valueSerializer);
            builder = builder.withCacheConfiguration(cacheName, cacheConfig);
        }
        return builder.build();
    }
}