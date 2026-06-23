package com.codeit.team5.mopl.watcher.service;

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
import com.codeit.team5.mopl.watcher.mapper.WatchingSessionMapper;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.ScrollPosition.Direction;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WatchingSessionService {

    private final WatchingSessionRepository repository;
    private final WatchingSessionMapper mapper;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    @Transactional
    public WatchingSessionResponse create(WatchingSessionCreatedRequest request) {
        if (!userRepository.existsById(request.watcherId())) {
            throw new IllegalArgumentException();
        }
        if (!contentRepository.existsById(request.contentId())) {
            throw new IllegalArgumentException();
        }
        User user = userRepository.getReferenceById(request.watcherId());
        Content content = contentRepository.getReferenceById(request.contentId());
        WatchingSession session = WatchingSession.of(user, content);
        repository.save(session);
        return mapper.toDto(session);
    }

    public WatchingSessionResponse findSessionByWatchId(UUID watcherId) {
        return mapper.toDto(repository.findByUser_Id(watcherId).orElseThrow());
    }

    public CursorResponse<WatchingSessionResponse> findSessionByContentId(UUID contentId,
            WatchingSessionCursorRequest request) {

        Sort sort = Sort.by(request.sortDirection(), request.sortBy().getValue())
                .and(Sort.by(request.sortDirection(), SortByType.ID.getValue()));
        ScrollPosition scrollPosition = createScrollPosition(request);

        Window<WatchingSession> result = repository.findByContent_Id(
                contentId, scrollPosition, Limit.of(request.limit()), sort);
        return mapper.toCursor(result);
    }

    @Transactional
    public void delete(UUID userId) {
        if (repository.existsByUser_Id(userId)) {
            repository.deleteByUserIdDirectly(userId);
            return;
        }
        throw new IllegalArgumentException();
    }

    private ScrollPosition createScrollPosition(WatchingSessionCursorRequest request) {
        if (request.cursor() == null || request.idAfter() == null) {
            return ScrollPosition.keyset();
        }
        Map<String, Object> keyset = Map.of(
                request.sortBy().getValue(), request.cursor(),
                SortByType.ID.getValue(), UUID.fromString(request.idAfter())
        );
        return ScrollPosition.of(keyset, Direction.FORWARD);
    }
}
