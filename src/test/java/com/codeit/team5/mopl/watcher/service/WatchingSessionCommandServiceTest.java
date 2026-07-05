package com.codeit.team5.mopl.watcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import java.util.Optional;
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
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
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
        WatchingSession session = createDummySession(user, content);

        when(userRepository.findWithProfileImageById(watcherId)).thenReturn(Optional.of(user));
        when(contentRepository.findWithStatsAndTagsById(contentId))
                .thenReturn(Optional.of(content));
        when(repository.save(any(WatchingSession.class))).thenReturn(session);
        when(repository.countByContentId(contentId)).thenReturn(5L);
        WatchingSessionResponse dummyResponse =
                WatchingSessionResponse.builder().id(UUID.randomUUID()).build();
        when(mapper.toDto(any(WatchingSession.class))).thenReturn(dummyResponse);

        // when
        WatchingSessionPayload result = service.join(contentId, watcherId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(WatcherStatus.JOIN);
        assertThat(result.response()).isEqualTo(dummyResponse);
        assertThat(result.watcherCount()).isEqualTo(5L);
        verify(repository).save(any(WatchingSession.class));
        verify(eventPublisher).publishEvent(any(WatcherJoinedEvent.class));
    }

    @Test
    @DisplayName("유저가 존재하지 않을 때 세션 생성")
    void join_UserNotFound() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        when(userRepository.findWithProfileImageById(watcherId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.join(contentId, watcherId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("컨텐츠가 존재하지 않을 때 세션 생성")
    void join_ContentNotFound() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        User user = createDummyUser("test@test.com");

        when(userRepository.findWithProfileImageById(watcherId)).thenReturn(Optional.of(user));
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.join(contentId, watcherId))
                .isInstanceOf(ContentNotFoundException.class);
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
        Content content = createDummyContent(contentId);
        WatchingSession session = createDummySession(user, content);

        when(repository.findByWatcherId(watcherId)).thenReturn(Optional.of(session));
        WatchingSessionResponse dummyResponse =
                WatchingSessionResponse.builder().id(UUID.randomUUID()).build();
        when(mapper.toDto(any(WatchingSession.class))).thenReturn(dummyResponse);
        when(repository.countByContentId(contentId)).thenReturn(4L);

        // when
        WatchingSessionPayload result = service.left(watcherId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(WatcherStatus.LEAVE);
        assertThat(result.response()).isEqualTo(dummyResponse);
        assertThat(result.watcherCount()).isEqualTo(4L);
        verify(repository).deleteByWatcherIdDirectly(watcherId);
    }

    @Test
    @DisplayName("유저 ID에 해당하는 세션이 없을 때 세션 삭제")
    void left_NotFound() {
        // given
        UUID watcherId = UUID.randomUUID();
        when(repository.findByWatcherId(watcherId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.left(watcherId))
                .isInstanceOf(WatchingSessionNotFoundException.class);
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

    private WatchingSession createDummySession(User user, Content content) {
        WatchingSession session = WatchingSession.of(user, content);
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }
}
