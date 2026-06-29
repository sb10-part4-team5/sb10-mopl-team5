package com.codeit.team5.mopl.watcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.constant.SortByType;
import com.codeit.team5.mopl.watcher.dto.request.WatchingSessionCursorRequest;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.mapper.entity.WatchingSessionMapper;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WatchingSessionServiceTest {

    @Mock
    private WatchingSessionRepository repository;

    @Mock
    private WatchingSessionMapper mapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private WatchingSessionService service;

    // --- CREATE ---
    @Test
    @DisplayName("세션 생성_성공")
    void create_성공() {
        // given
        String email = "test@test.com";
        UUID contentId = UUID.randomUUID();

        User user = createDummyUser(email);
        Content content = createDummyContent(contentId);
        WatchingSession session = createDummySession(user, content);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(
                Optional.of(content));
        when(repository.save(any(WatchingSession.class))).thenReturn(session);
        when(mapper.toDto(any(WatchingSession.class))).thenReturn(
                WatchingSessionResponse.builder().id(UUID.randomUUID()).build());

        // when
        WatchingSessionResponse result = service.create(contentId, email);

        // then
        assertThat(result).isNotNull();
        verify(repository).save(any(WatchingSession.class));
    }

    @Test
    @DisplayName("유저가 존재하지 않을 때 세션 생성")
    void create_UserNotFound() {
        // given
        String email = "test@test.com";
        UUID contentId = UUID.randomUUID();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.create(contentId, email))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("컨텐츠가 존재하지 않을 때 세션 생성")
    void create_ContentNotFound() {
        // given
        String email = "test@test.com";
        UUID contentId = UUID.randomUUID();
        User user = createDummyUser(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.create(contentId, email))
                .isInstanceOf(ContentNotFoundException.class);
    }

    // --- READ (findSessionByWatchId) ---
    @Test
    @DisplayName("워쳐 ID로 세션 조회_성공")
    void findSessionByWatchId_성공() {
        // given
        UUID watcherId = UUID.randomUUID();
        User user = createDummyUser("test@test.com");
        Content content = createDummyContent(UUID.randomUUID());
        WatchingSession session = createDummySession(user, content);

        when(repository.findByWatcherId(watcherId)).thenReturn(Optional.of(session));
        when(mapper.toDto(any(WatchingSession.class))).thenReturn(
                WatchingSessionResponse.builder().id(UUID.randomUUID()).build());

        // when
        WatchingSessionResponse result = service.findSessionByWatchId(watcherId);

        // then
        assertThat(result).isNotNull();
        verify(repository).findByWatcherId(watcherId);
    }

    @Test
    @DisplayName("존재하지 않는 워쳐 ID로 세션 조회")
    void findSessionByWatchId_NotFound() {
        // given
        UUID watcherId = UUID.randomUUID();
        when(repository.findByWatcherId(watcherId)).thenReturn(Optional.empty());

        // when
        WatchingSessionResponse result = service.findSessionByWatchId(watcherId);

        // then
        assertThat(result).isNull();
    }

    // --- READ (findSessionByContentId) ---
    @Test
    @DisplayName("컨텐츠 ID로 첫 페이지 커서 기반 세션 조회_성공")
    void findSessionByContentId_FirstPage_성공() {
        // given
        UUID contentId = UUID.randomUUID();
        WatchingSessionCursorRequest request = new WatchingSessionCursorRequest(
                null, null, null, 10, Sort.Direction.DESC, SortByType.CREATED_AT);

        @SuppressWarnings("unchecked")
        Window<WatchingSession> window = mock(Window.class);

        when(repository.findByContentId(eq(contentId), any(ScrollPosition.class), any(Limit.class),
                any(Sort.class)))
                .thenReturn(window);
        when(repository.countByContentId(contentId)).thenReturn(1L);
        when(mapper.toCursor(any(), any(), any(), any())).thenReturn(
                new CursorResponse<>(null, null, null, false, 1L, null, null));

        // when
        CursorResponse<WatchingSessionResponse> result = service.findSessionByContentId(contentId,
                request);

        // then
        assertThat(result).isNotNull();

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findByContentId(eq(contentId), any(ScrollPosition.class),
                any(Limit.class), sortCaptor.capture());

        Sort capturedSort = sortCaptor.getValue();
        assertThat(capturedSort.getOrderFor(SortByType.CREATED_AT.getValue())
                .getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(capturedSort.getOrderFor("id").getDirection()).isEqualTo(
                Sort.Direction.DESC);
    }

    @Test
    @DisplayName("컨텐츠 ID로 다음 페이지 커서 기반 세션 조회_성공")
    void findSessionByContentId_NextPage_성공() {
        // given
        UUID contentId = UUID.randomUUID();
        WatchingSessionCursorRequest request = new WatchingSessionCursorRequest(
                null,
                "cursorValue",
                UUID.randomUUID().toString(),
                10,
                Sort.Direction.DESC,
                SortByType.CREATED_AT);

        @SuppressWarnings("unchecked")
        Window<WatchingSession> window = mock(Window.class);

        when(repository.findByContentId(eq(contentId), any(ScrollPosition.class), any(Limit.class),
                any(Sort.class)))
                .thenReturn(window);
        when(repository.countByContentId(contentId)).thenReturn(11L);
        when(mapper.toCursor(any(), any(), any(), any())).thenReturn(
                new CursorResponse<>(null, null, null, false, 11L, null, null));

        // when
        CursorResponse<WatchingSessionResponse> result = service.findSessionByContentId(contentId,
                request);

        // then
        assertThat(result).isNotNull();

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findByContentId(eq(contentId), any(ScrollPosition.class),
                any(Limit.class), sortCaptor.capture());

        Sort capturedSort = sortCaptor.getValue();
        assertThat(capturedSort.getOrderFor(SortByType.CREATED_AT.getValue())
                .getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(capturedSort.getOrderFor("id").getDirection()).isEqualTo(
                Sort.Direction.DESC);
    }

    // --- DELETE ---
    @Test
    @DisplayName("세션 삭제_성공")
    void delete_성공() {
        // given
        String email = "test@test.com";
        when(repository.existsByWatcherEmail(email)).thenReturn(true);

        // when
        service.delete(email);

        // then
        verify(repository).deleteByWatcherEmailDirectly(email);
    }

    @Test
    @DisplayName("유저 ID에 해당하는 세션이 없을 때 세션 삭제")
    void delete_NotFound() {
        // given
        String email = "notfound@test.com";
        when(repository.existsByWatcherEmail(email)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.delete(email))
                .isInstanceOf(WatchingSessionNotFoundException.class);
    }

    private User createDummyUser(String email) {
        User user = User.create(email, "password", "testName");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Content createDummyContent(UUID id) {
        Content content = Content.createByExternalSource(
                ContentType.MOVIE,
                "test title",
                null,
                ContentSource.TMDB,
                "ext-id-123",
                null,
                null
        );
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }

    private WatchingSession createDummySession(User user, Content content) {
        WatchingSession session = WatchingSession.of(user, content);
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }
}
