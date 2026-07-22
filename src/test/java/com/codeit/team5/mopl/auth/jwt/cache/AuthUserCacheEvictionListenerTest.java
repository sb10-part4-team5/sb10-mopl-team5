package com.codeit.team5.mopl.auth.jwt.cache;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthUserCacheEvictionListenerTest {

    @Mock
    private AuthUserCacheStore authUserCacheStore;

    @InjectMocks
    private AuthUserCacheEvictionListener listener;

    @Test
    void handleRoleChangedEvent_evictsReceiverCache() {
        UUID userId = UUID.randomUUID();

        listener.handle(new RoleChangedEvent(userId, "USER", "ADMIN"));

        verify(authUserCacheStore).evict(userId);
    }

    @Test
    void handleLockedEvent_evictsUserCache() {
        UUID userId = UUID.randomUUID();

        listener.handle(new UserLockedEvent(userId, true));

        verify(authUserCacheStore).evict(userId);
    }

    @Test
    void handleUnlockedEvent_evictsUserCache() {
        UUID userId = UUID.randomUUID();

        listener.handle(new UserLockedEvent(userId, false));

        verify(authUserCacheStore).evict(userId);
    }

    @Test
    void handle_propagatesCacheEvictionFailure() {
        UUID userId = UUID.randomUUID();
        RuntimeException failure = new RuntimeException("redis unavailable");
        doThrow(failure).when(authUserCacheStore).evict(userId);

        assertThatThrownBy(() -> listener.handle(new UserLockedEvent(userId, true)))
                .isSameAs(failure);

        verify(authUserCacheStore).evict(userId);
    }
}
