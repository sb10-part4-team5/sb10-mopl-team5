package com.codeit.team5.mopl.auth.jwt.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipalService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthUserCacheStoreTest {

    @Mock
    private MoplPrincipalService moplPrincipalService;

    private SimpleMeterRegistry meterRegistry;
    private AuthUserCacheStore store;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        store = new AuthUserCacheStore(moplPrincipalService, meterRegistry);
    }

    @Test
    void getByUserId_returnsLoadedUserAndRecordsSuccessTimer() {
        UUID userId = UUID.randomUUID();
        AuthUser expected = new AuthUser(userId, "user@example.com", "USER", false);
        given(moplPrincipalService.loadAuthUserById(userId)).willReturn(expected);

        AuthUser actual = store.getByUserId(userId);

        assertThat(actual).isSameAs(expected);
        verify(moplPrincipalService).loadAuthUserById(userId);
        assertThat(meterRegistry.get("auth.user.lookup")
                .tags("source", "db", "result", "success")
                .timer()
                .count()).isEqualTo(1);
    }

    @Test
    void getByUserId_propagatesFailureAndRecordsFailureTimer() {
        UUID userId = UUID.randomUUID();
        RuntimeException failure = new RuntimeException("lookup failed");
        given(moplPrincipalService.loadAuthUserById(userId)).willThrow(failure);

        assertThatThrownBy(() -> store.getByUserId(userId)).isSameAs(failure);

        verify(moplPrincipalService).loadAuthUserById(userId);
        assertThat(meterRegistry.get("auth.user.lookup")
                .tags("source", "db", "result", "failure")
                .timer()
                .count()).isEqualTo(1);
    }

    @Test
    void evict_completesWithoutException() {
        assertThatCode(() -> store.evict(UUID.randomUUID())).doesNotThrowAnyException();
    }
}
