package com.codeit.team5.mopl.watcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.constant.SortByType;
import com.codeit.team5.mopl.watcher.dto.WatchingSessionCreatedRequest;
import com.codeit.team5.mopl.watcher.dto.WatchingSessionCursorRequest;
import com.codeit.team5.mopl.watcher.dto.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.exception.WatcherErrorCode;
import com.codeit.team5.mopl.watcher.exception.WatcherException;
import com.codeit.team5.mopl.watcher.mapper.WatchingSessionMapper;
import com.codeit.team5.mopl.watcher.mapper.WatchingSessionMapperImpl;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

    @Spy
    private WatchingSessionMapper mapper = new WatchingSessionMapperImpl();

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
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        WatchingSessionCreatedRequest request = new WatchingSessionCreatedRequest(watcherId,
                contentId);

        User user = createDummyUser(watcherId);
        Content content = createDummyContent(contentId);
        WatchingSession session = createDummySession(user, content);

        when(userRepository.existsById(watcherId)).thenReturn(true);
        when(contentRepository.existsById(contentId)).thenReturn(true);
        when(userRepository.getReferenceById(watcherId)).thenReturn(user);
        when(contentRepository.getReferenceById(contentId)).thenReturn(content);
        when(repository.save(any(WatchingSession.class))).thenReturn(session);

        // when
        WatchingSessionResponse result = service.create(request);

        // then
        assertThat(result).isNotNull();
        verify(repository).save(any(WatchingSession.class));
    }

    @Test
    @DisplayName("유저가 존재하지 않을 때 세션 생성_실패")
    void create_UserNotFound_실패() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        WatchingSessionCreatedRequest request = new WatchingSessionCreatedRequest(watcherId,
                contentId);

        when(userRepository.existsById(watcherId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(WatcherException.class)
                .hasMessage(WatcherErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("컨텐츠가 존재하지 않을 때 세션 생성_실패")
    void create_ContentNotFound_실패() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        WatchingSessionCreatedRequest request = new WatchingSessionCreatedRequest(watcherId,
                contentId);

        when(userRepository.existsById(watcherId)).thenReturn(true);
        when(contentRepository.existsById(contentId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(WatcherException.class)
                .hasMessage(WatcherErrorCode.CONTENT_NOT_FOUND.getMessage());
    }

    // --- READ (findSessionByWatchId) ---
    @Test
    @DisplayName("워쳐 ID로 세션 조회_성공")
    void findSessionByWatchId_성공() {
        // given
        UUID watcherId = UUID.randomUUID();
        User user = createDummyUser(watcherId);
        Content content = createDummyContent(UUID.randomUUID());
        WatchingSession session = createDummySession(user, content);

        when(repository.findByUser_Id(watcherId)).thenReturn(Optional.of(session));

        // when
        WatchingSessionResponse result = service.findSessionByWatchId(watcherId);

        // then
        assertThat(result).isNotNull();
        verify(repository).findByUser_Id(watcherId);
    }

    @Test
    @DisplayName("존재하지 않는 워쳐 ID로 세션 조회_실패")
    void findSessionByWatchId_NotFound_실패() {
        // given
        UUID watcherId = UUID.randomUUID();
        when(repository.findByUser_Id(watcherId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.findSessionByWatchId(watcherId))
                .isInstanceOf(WatcherException.class)
                .hasMessage(WatcherErrorCode.WATCHING_SESSION_NOT_FOUND.getMessage());
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

        when(repository.findByContent_Id(eq(contentId), any(ScrollPosition.class), any(Limit.class),
                any(Sort.class)))
                .thenReturn(window);

        // when
        CursorResponse<WatchingSessionResponse> result = service.findSessionByContentId(contentId,
                request);

        // then
        assertThat(result).isNotNull();

        org.mockito.ArgumentCaptor<Sort> sortCaptor = org.mockito.ArgumentCaptor.forClass(
                Sort.class);
        verify(repository).findByContent_Id(eq(contentId), any(ScrollPosition.class),
                any(Limit.class), sortCaptor.capture());

        Sort capturedSort = sortCaptor.getValue();
        assertThat(capturedSort.getOrderFor(SortByType.CREATED_AT.getValue())
                .getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(capturedSort.getOrderFor(SortByType.ID.getValue()).getDirection()).isEqualTo(
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

        when(repository.findByContent_Id(eq(contentId), any(ScrollPosition.class), any(Limit.class),
                any(Sort.class)))
                .thenReturn(window);

        // when
        CursorResponse<WatchingSessionResponse> result = service.findSessionByContentId(contentId,
                request);

        // then
        assertThat(result).isNotNull();

        org.mockito.ArgumentCaptor<Sort> sortCaptor = org.mockito.ArgumentCaptor.forClass(
                Sort.class);
        verify(repository).findByContent_Id(eq(contentId), any(ScrollPosition.class),
                any(Limit.class), sortCaptor.capture());

        Sort capturedSort = sortCaptor.getValue();
        assertThat(capturedSort.getOrderFor(SortByType.CREATED_AT.getValue())
                .getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(capturedSort.getOrderFor(SortByType.ID.getValue()).getDirection()).isEqualTo(
                Sort.Direction.DESC);
    }

    // --- DELETE ---
    @Test
    @DisplayName("세션 삭제_성공")
    void delete_성공() {
        // given
        UUID watcherId = UUID.randomUUID();
        when(repository.existsByUser_Id(watcherId)).thenReturn(true);

        // when
        service.delete(watcherId);

        // then
        verify(repository).deleteByUserIdDirectly(watcherId);
    }

    @Test
    @DisplayName("유저 ID에 해당하는 세션이 없을 때 세션 삭제_실패")
    void delete_NotFound_실패() {
        // given
        UUID watcherId = UUID.randomUUID();
        when(repository.existsByUser_Id(watcherId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.delete(watcherId))
                .isInstanceOf(WatcherException.class)
                .hasMessage(WatcherErrorCode.WATCHING_SESSION_NOT_FOUND.getMessage());
    }

    private User createDummyUser(UUID id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content createDummyContent(UUID id) {
        Content content = new Content();
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }

    private WatchingSession createDummySession(User user, Content content) {
        WatchingSession session = WatchingSession.of(user, content);
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }
}
