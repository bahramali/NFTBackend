package se.hydroleaf.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${cache.aggregatedHistory.ttl:5m}") Duration aggregatedHistoryTtl,
            @Value("${cache.liveNow.ttl:2s}") Duration liveNowTtl
    ) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of("aggregatedHistory", "liveNow"));
        manager.registerCustomCache("aggregatedHistory",
                Caffeine.newBuilder()
                        .expireAfterWrite(aggregatedHistoryTtl)
                        .maximumSize(500)
                        .build());
        manager.registerCustomCache("liveNow",
                Caffeine.newBuilder()
                        .expireAfterWrite(liveNowTtl)
                        .maximumSize(500)
                        .build());
        return manager;
    }
}
