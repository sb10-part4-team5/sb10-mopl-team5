package com.codeit.team5.mopl.watcher.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.content.event.ContentDeletedEvent;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchingSessionEventListenerTest {

    @Mock
    private WatchingSessionCommandService commandService;
    
    @Mock
    private WatchingSessionRepository redisRepository;

    @InjectMocks
    private WatchingSessionEventListener listener;

    @Test
    @DisplayName("onContentDeleted - 연관된 세션 삭제")
    void onContentDeleted_Success() {
        // given
        UUID contentId = UUID.randomUUID();
        ContentDeletedEvent event = new ContentDeletedEvent(contentId);

        // when
        listener.onContentDeleted(event);

        // then
        verify(commandService).clearContentSessions(contentId);
    }

    @Test
    @DisplayName("onUserLocked - 유저가 잠금 처리되었고 세션이 있다면 세션 이탈")
    void onUserLocked_LockedAndSessionExists() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockedEvent event = new UserLockedEvent(userId, true);
        UUID contentId = UUID.randomUUID();
        WatchingSession session = new WatchingSession(userId, contentId, Instant.now());
        
        when(redisRepository.findByWatcherId(userId)).thenReturn(Optional.of(session));

        // when
        listener.onUserLocked(event);

        // then
        verify(commandService).left(contentId, userId);
    }

    @Test
    @DisplayName("onUserLocked - 유저가 잠금 처리되었으나 세션이 없다면 무시")
    void onUserLocked_LockedAndNoSession() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockedEvent event = new UserLockedEvent(userId, true);
        
        when(redisRepository.findByWatcherId(userId)).thenReturn(Optional.empty());

        // when
        listener.onUserLocked(event);

        // then
        verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("onUserLocked - 유저 잠금 해제 이벤트는 무시")
    void onUserLocked_Unlocked() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockedEvent event = new UserLockedEvent(userId, false);

        // when
        listener.onUserLocked(event);

        // then
        verifyNoInteractions(redisRepository);
        verifyNoInteractions(commandService);
    }
}
