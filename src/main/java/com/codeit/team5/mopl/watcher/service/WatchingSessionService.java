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
import com.codeit.team5.mopl.watcher.exception.WatchingSessionContentNotFound;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionUserNotFoundException;
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

    private static final String SECONDARY_SORT_FIELD = "id";

    @Transactional
    public WatchingSessionResponse create(WatchingSessionCreatedRequest request) {
        if (!userRepository.existsById(request.watcherId())) {
            throw new WatchingSessionUserNotFoundException(request.watcherId());
        }
        if (!contentRepository.existsById(request.contentId())) {
            throw new WatchingSessionContentNotFound(request.contentId());
        }
        User user = userRepository.getReferenceById(request.watcherId());
        Content content = contentRepository.getReferenceById(request.contentId());
        WatchingSession session = WatchingSession.of(user, content);
        repository.save(session);
        return mapper.toDto(session);
    }

    public WatchingSessionResponse findSessionByWatchId(UUID watcherId) {
        return mapper.toDto(repository.findByUserId(watcherId)
                .orElseThrow(() -> new WatchingSessionNotFoundException(watcherId)));
    }

    public CursorResponse<WatchingSessionResponse> findSessionByContentId(UUID contentId,
            WatchingSessionCursorRequest request) {
        Sort.Direction direction = request.sortDirection();
        SortByType sortBy = request.sortBy();
        Sort sort = Sort.by(direction, sortBy.getValue())
                .and(Sort.by(direction, SECONDARY_SORT_FIELD));
        ScrollPosition scrollPosition = createScrollPosition(request);
        Window<WatchingSession> result = repository.findByContentId(
                contentId, scrollPosition, Limit.of(request.limit()), sort);
        Long totalCount = repository.countByContentId(contentId);
        return mapper.toCursor(result, totalCount, sortBy, direction);
    }

    @Transactional
    public void delete(UUID userId) {
        if (repository.existsByUserId(userId)) {
            repository.deleteByUserIdDirectly(userId);
            return;
        }
        throw new WatchingSessionUserNotFoundException(userId);
    }

    private ScrollPosition createScrollPosition(WatchingSessionCursorRequest request) {
        String cursor = request.cursor();
        String idAfter = request.idAfter();
        if (cursor == null || idAfter == null) {
            return ScrollPosition.keyset();
        }
        Map<String, Object> keyset = Map.of(
                request.sortBy().getValue(), cursor,
                SECONDARY_SORT_FIELD, UUID.fromString(idAfter)
        );
        return ScrollPosition.of(keyset, Direction.FORWARD);
    }
}
