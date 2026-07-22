package com.codeit.team5.mopl.auth.jwt.cache;

import static com.codeit.team5.mopl.global.infra.redis.config.RedisCacheConfig.AUTH_USER_CACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipalService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthUserCacheProxyIntegrationTest {

    @Autowired
    private AuthUserCacheStore store;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private MoplPrincipalService moplPrincipalService;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache(AUTH_USER_CACHE).clear();
    }

    @Test
    void getByUserId_usesCachedValueOnSecondCall() {
        UUID userId = UUID.randomUUID();
        AuthUser expected = authUser(userId, "first@example.com");
        given(moplPrincipalService.loadAuthUserById(userId)).willReturn(expected);

        assertThat(store.getByUserId(userId)).isEqualTo(expected);
        assertThat(store.getByUserId(userId)).isEqualTo(expected);

        verify(moplPrincipalService).loadAuthUserById(userId);
    }

    @Test
    void evict_removesCachedValue() {
        UUID userId = UUID.randomUUID();
        AuthUser beforeEviction = authUser(userId, "before@example.com");
        AuthUser afterEviction = authUser(userId, "after@example.com");
        given(moplPrincipalService.loadAuthUserById(userId))
                .willReturn(beforeEviction, afterEviction);

        assertThat(store.getByUserId(userId)).isEqualTo(beforeEviction);
        store.evict(userId);
        assertThat(store.getByUserId(userId)).isEqualTo(afterEviction);

        verify(moplPrincipalService, times(2)).loadAuthUserById(userId);
    }

    @Test
    void getByUserId_keepsCacheEntriesSeparatedByUserId() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        AuthUser first = authUser(firstUserId, "first@example.com");
        AuthUser second = authUser(secondUserId, "second@example.com");
        given(moplPrincipalService.loadAuthUserById(firstUserId)).willReturn(first);
        given(moplPrincipalService.loadAuthUserById(secondUserId)).willReturn(second);

        assertThat(store.getByUserId(firstUserId)).isEqualTo(first);
        assertThat(store.getByUserId(secondUserId)).isEqualTo(second);
        assertThat(store.getByUserId(firstUserId)).isEqualTo(first);
        assertThat(store.getByUserId(secondUserId)).isEqualTo(second);

        verify(moplPrincipalService).loadAuthUserById(firstUserId);
        verify(moplPrincipalService).loadAuthUserById(secondUserId);
    }

    private AuthUser authUser(UUID userId, String email) {
        return new AuthUser(userId, email, "USER", false);
    }
}
