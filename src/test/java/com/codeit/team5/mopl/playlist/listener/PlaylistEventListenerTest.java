package com.codeit.team5.mopl.playlist.listener;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.user.event.UserLockedEvent;

@ExtendWith(MockitoExtension.class)
class PlaylistEventListenerTest {

    @Mock
    private PlaylistRepository repository;

    @InjectMocks
    private PlaylistEventListener listener;

    @Test
    @DisplayName("유저 락(Lock) 이벤트가 발생하면 구독 중인 플리의 구독자 수가 감소한다")
    void handle_UserLockedEvent_Locked() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockedEvent event = new UserLockedEvent(userId, true);

        // when
        listener.handle(event);

        // then
        verify(repository).bulkDecreaseSubscribeCountBySubscriberId(userId);
    }

    @Test
    @DisplayName("유저 언락(Unlock) 이벤트가 발생하면 구독 중인 플리의 구독자 수가 복구(증가)된다")
    void handle_UserLockedEvent_Unlocked() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockedEvent event = new UserLockedEvent(userId, false);

        // when
        listener.handle(event);

        // then
        verify(repository).bulkIncreaseSubscribeCountBySubscriberId(userId);
    }
}
