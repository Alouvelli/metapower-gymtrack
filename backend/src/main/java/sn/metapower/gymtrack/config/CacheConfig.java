package sn.metapower.gymtrack.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache "scan-dedup" : empêche qu'un même QR scanné plusieurs fois en moins de 5 minutes
 * sur le même site déclenche plusieurs pointages successifs.
 */
@Configuration
public class CacheConfig {

    public static final String SCAN_DEDUP_CACHE = "scan-dedup";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(SCAN_DEDUP_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10_000));
        return manager;
    }
}
