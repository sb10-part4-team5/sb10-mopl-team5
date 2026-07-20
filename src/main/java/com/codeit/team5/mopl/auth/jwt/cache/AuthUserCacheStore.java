package com.codeit.team5.mopl.auth.jwt.cache;

import static com.codeit.team5.mopl.global.infra.redis.config.RedisCacheConfig.AUTH_USER_CACHE;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipalService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthUserCacheStore {

    private static final String AUTH_USER_LOOKUP_TIMER =
            "auth.user.lookup";

    private final MoplPrincipalService moplPrincipalService;
    private final MeterRegistry meterRegistry;

    @Cacheable(
            value = AUTH_USER_CACHE,
            key = "#userId.toString()",
            sync = true
    )
    public AuthUser getByUserId(UUID userId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "success";

        try {
            return moplPrincipalService.loadAuthUserById(userId);
        } catch (RuntimeException e) {
            result = "failure";
            throw e;
        } finally {
            sample.stop(
                    meterRegistry.timer(
                            AUTH_USER_LOOKUP_TIMER,
                            "source", "db",
                            "result", result
                    )
            );
        }
    }

    @CacheEvict(
            value = AUTH_USER_CACHE,
            key = "#userId.toString()"
    )
    public void evict(UUID userId) {
    }
}
