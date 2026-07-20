package com.codeit.team5.mopl.watcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.constant.WatcherSortByType;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.request.WatchingSessionCursorRequest;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.mapper.entity.WatchingSessionMapper;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;

@ExtendWith(MockitoExtension.class)
class WatchingSessionQueryServiceTest {

    @Mock
    private WatchingSessionRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private WatchingSessionMapper mapper;

    @InjectMocks
    private WatchingSessionQueryService service;

    // --- READ (findByWatcherId) ---
    @Test
    @DisplayName("워쳐 ID로 세션 조회_성공")
    void findByWatcherId_성공() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        WatchingSession session = new WatchingSession(watcherId, contentId, Instant.now());
        User user = mock(User.class);
        Content content = mock(Content.class);

        when(repository.findByWatcherId(watcherId)).thenReturn(Optional.of(session));
        when(userRepository.findWithProfileImageById(watcherId)).thenReturn(Optional.of(user));
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(mapper.toDto(session, user, content))
                .thenReturn(WatchingSessionResponse.builder().id(watcherId).build());

        // when
        WatchingSessionResponse result = service.findByWatcherId(watcherId);

        // then
        assertThat(result).isNotNull();
        verify(repository).findByWatcherId(watcherId);
    }

    @Test
    @DisplayName("존재하지 않는 워쳐 ID로 세션 조회")
    void findByWatcherId_NotFound() {
        // given
        UUID watcherId = UUID.randomUUID();
        when(repository.findByWatcherId(watcherId)).thenReturn(Optional.empty());

        // when
        WatchingSessionResponse result = service.findByWatcherId(watcherId);

        // then
        assertThat(result).isNull();
    }

    // --- READ (findCursorByContentId) ---
    @Test
    @DisplayName("컨텐츠 ID로 첫 페이지 커서 기반 세션 조회_성공")
    void findCursorByContentId_FirstPage_성공() {
        // given
        UUID contentId = UUID.randomUUID();
        WatchingSessionCursorRequest request = new WatchingSessionCursorRequest(null, null, null,
                10, Sort.Direction.DESC, WatcherSortByType.CREATED_AT.getValue());

        UUID watcherId = UUID.randomUUID();
        WatchingSession session = new WatchingSession(watcherId, contentId, Instant.now());
        List<WatchingSession> sessions = List.of(session);
        
        User user = mock(User.class);
        when(user.getId()).thenReturn(watcherId);
        Content content = mock(Content.class);

        when(repository.findWatchingSessionsByContentId(eq(contentId), eq(11), eq(Double.MAX_VALUE)))
                .thenReturn(sessions);
        when(userRepository.findWithProfileImageByIdIn(any(Set.class))).thenReturn(List.of(user));
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(repository.countByContentId(contentId)).thenReturn(1L);
        when(mapper.toCursor(any(), eq(false), eq(1L), eq(request.sortBy()), eq(request.sortDirection().toString())))
                .thenReturn(new CursorResponse<>(null, null, null, false, 1L, null, null));

        // when
        CursorResponse<WatchingSessionResponse> result =
                service.findCursorByContentId(contentId, request);

        // then
        assertThat(result).isNotNull();
        verify(repository).findWatchingSessionsByContentId(eq(contentId), eq(11), eq(Double.MAX_VALUE));
    }

    @Test
    @DisplayName("컨텐츠 ID로 다음 페이지 커서 기반 세션 조회_성공")
    void findCursorByContentId_NextPage_성공() {
        // given
        UUID contentId = UUID.randomUUID();
        Instant cursorTime = Instant.now();
        WatchingSessionCursorRequest request =
                new WatchingSessionCursorRequest(null, cursorTime, UUID.randomUUID(), 10,
                        Sort.Direction.DESC, WatcherSortByType.CREATED_AT.getValue());

        UUID watcherId = UUID.randomUUID();
        WatchingSession session = new WatchingSession(watcherId, contentId, cursorTime.minusSeconds(1));
        List<WatchingSession> sessions = List.of(session);
        
        User user = mock(User.class);
        when(user.getId()).thenReturn(watcherId);
        Content content = mock(Content.class);

        double expectedMaxScore = (double) cursorTime.toEpochMilli();

        when(repository.findWatchingSessionsByContentId(eq(contentId), eq(11), eq(expectedMaxScore)))
                .thenReturn(sessions);
        when(userRepository.findWithProfileImageByIdIn(any(Set.class))).thenReturn(List.of(user));
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        when(repository.countByContentId(contentId)).thenReturn(11L);
        when(mapper.toCursor(any(), eq(false), eq(11L), eq(request.sortBy()), eq(request.sortDirection().toString())))
                .thenReturn(new CursorResponse<>(null, null, null, false, 11L, null, null));

        // when
        CursorResponse<WatchingSessionResponse> result =
                service.findCursorByContentId(contentId, request);

        // then
        assertThat(result).isNotNull();
        verify(repository).findWatchingSessionsByContentId(eq(contentId), eq(11), eq(expectedMaxScore));
    }

    // --- READ (ensureWatchingContent) ---
    @Test
    @DisplayName("시청중인 세션 확인 성공")
    void ensureWatchingContent_성공() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(repository.existsByContentIdAndWatcherId(contentId, watcherId)).thenReturn(true);

        // when & then (예외가 발생하지 않으면 성공)
        service.ensureWatchingContent(contentId, watcherId);
        verify(repository).existsByContentIdAndWatcherId(contentId, watcherId);
    }

    @Test
    @DisplayName("시청중인 세션이 아니면 예외 발생")
    void ensureWatchingContent_예외발생() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        when(repository.existsByContentIdAndWatcherId(contentId, watcherId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.ensureWatchingContent(contentId, watcherId))
                .isInstanceOf(WatchingSessionNotFoundException.class);
    }

    // --- READ (getWatchingSessionPayload) ---
    @Test
    @DisplayName("워쳐 ID와 상태로 세션 페이로드 조회_성공")
    void getWatchingSessionPayload_성공() {
        // given
        UUID watcherId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        WatcherStatus status = WatcherStatus.JOIN;
        WatchingSession session = new WatchingSession(watcherId, contentId, Instant.now());
        User user = mock(User.class);
        Content content = mock(Content.class);

        when(repository.findByWatcherId(watcherId)).thenReturn(Optional.of(session));
        // mocked for findByWatcherId call within getWatchingSessionPayload
        when(userRepository.findWithProfileImageById(watcherId)).thenReturn(Optional.of(user));
        when(contentRepository.findWithStatsAndTagsById(contentId)).thenReturn(Optional.of(content));
        
        when(repository.countByContentId(contentId)).thenReturn(5L);

        // when
        WatchingSessionPayload result = service.getWatchingSessionPayload(watcherId, status);

        // then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.watcherCount()).isEqualTo(5L);
        verify(repository).countByContentId(contentId);
    }

    @Test
    @DisplayName("존재하지 않는 워쳐 ID로 세션 페이로드 조회 시 예외 발생")
    void getWatchingSessionPayload_NotFound_예외발생() {
        // given
        UUID watcherId = UUID.randomUUID();
        WatcherStatus status = WatcherStatus.JOIN;

        when(repository.findByWatcherId(watcherId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getWatchingSessionPayload(watcherId, status))
                .isInstanceOf(WatchingSessionNotFoundException.class);
    }
}
