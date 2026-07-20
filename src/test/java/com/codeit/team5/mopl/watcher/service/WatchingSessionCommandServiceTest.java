package com.codeit.team5.mopl.watcher.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionContentNotFoundException;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionUserNotFoundException;
import com.codeit.team5.mopl.watcher.mapper.entity.WatchingSessionMapper;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;

@ExtendWith(MockitoExtension.class)
class WatchingSessionCommandServiceTest {

    @Mock
    private WatchingSessionRepository repository;

    @Mock
    private WatchingSessionMapper mapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WatchingSessionCommandService service;

    // --- CREATE (join) ---
    @Test
    @DisplayName("세션 생성_성공")
    void join_성공() {
        // given
        UUID contentId = UUID.randomUUID();

        User user = createDummyUser("test@test.com");
        UUID watcherId = user.getId();
        Content content = createDummyContent(contentId);

        when(contentRepository.existsById(contentId)).thenReturn(true);
        when(userRepository.existsById(watcherId)).thenReturn(true);


        // when
        service.join(contentId, watcherId);

        // then
        verify(repository).save(any(WatchingSession.class));
        verify(eventPublisher).publishEvent(any(WatcherJoinedEvent.class));
    }

    @Test
    @DisplayName("유저가 존재하지 않을 때 세션 생성")
    void join_UserNotFound() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        when(contentRepository.existsById(contentId)).thenReturn(true);
        when(userRepository.existsById(watcherId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.join(contentId, watcherId))
                .isInstanceOf(WatchingSessionUserNotFoundException.class);
    }

    @Test
    @DisplayName("컨텐츠가 존재하지 않을 때 세션 생성")
    void join_ContentNotFound() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        when(contentRepository.existsById(contentId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.join(contentId, watcherId))
                .isInstanceOf(WatchingSessionContentNotFoundException.class);
    }

    // --- DELETE (left) ---
    @Test
    @DisplayName("세션 삭제_성공")
    void left_성공() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        User user = createDummyUser("test@test.com");
        ReflectionTestUtils.setField(user, "id", watcherId);

        when(repository.existsByContentIdAndWatcherId(contentId, watcherId)).thenReturn(true);

        // when
        service.left(contentId, watcherId);

        // then
        verify(repository).deleteByContentIdAndWatcherId(contentId, watcherId);
        verify(eventPublisher).publishEvent(any(WatcherLeftEvent.class));
    }

    @Test
    @DisplayName("유저 ID에 해당하는 세션이 없을 때 세션 삭제")
    void left_NotFound() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(repository.existsByContentIdAndWatcherId(contentId, watcherId)).thenReturn(false);

        // when
        service.left(contentId, watcherId);

        // then
        verify(repository, never()).deleteByContentIdAndWatcherId(contentId, watcherId);
        verify(eventPublisher, never()).publishEvent(any(WatcherLeftEvent.class));
    }

    private User createDummyUser(String email) {
        User user = User.create(email, "password", "testName");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Content createDummyContent(UUID id) {
        Content content = Content.createByExternalSource(ContentType.MOVIE, "test title", null,
                ContentSource.TMDB, "ext-id-123", null, null);
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }
}
