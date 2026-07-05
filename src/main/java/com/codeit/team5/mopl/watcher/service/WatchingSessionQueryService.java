package com.codeit.team5.mopl.watcher.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.ScrollPosition.Direction;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;
import com.codeit.team5.mopl.watcher.constant.WatcherSortByType;
import com.codeit.team5.mopl.watcher.dto.request.WatchingSessionCursorRequest;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.mapper.entity.WatchingSessionMapper;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ExecutionTracer
public class WatchingSessionQueryService {

    private final String secondarySortBy = "id";
    private final WatchingSessionRepository repository;
    private final WatchingSessionMapper mapper;

    public WatchingSessionResponse findByWatcherId(UUID watcherId) {
        return repository.findByWatcherId(watcherId).map(mapper::toDto).orElse(null);
    }

    public CursorResponse<WatchingSessionResponse> findCursorByContentId(UUID contentId,
            WatchingSessionCursorRequest request) {
        Sort.Direction direction = request.sortDirection();
        WatcherSortByType sortBy = request.sortBy();
        Sort sort =
                Sort.by(direction, sortBy.getValue()).and(Sort.by(direction, secondarySortBy));
        ScrollPosition scrollPosition = createScrollPosition(request);
        Window<WatchingSession> result = repository.findByContentId(contentId, scrollPosition,
                Limit.of(request.limit()), sort);
        Long totalCount = repository.countByContentId(contentId);
        return mapper.toCursor(result, totalCount, sortBy, direction);
    }

    public void ensureWatchingContent(UUID contentId, UUID watcherId) {
        if (!repository.existsByWatcherIdAndContentId(watcherId, contentId)) {
            throw new WatchingSessionNotFoundException(
                    Map.of("watcherId", watcherId, "contentId", contentId));
        }
    }

    private ScrollPosition createScrollPosition(WatchingSessionCursorRequest request) {
        Instant cursor = request.cursor();
        String idAfter = request.idAfter();
        if (cursor == null || idAfter == null) {
            return ScrollPosition.keyset();
        }
        Map<String, Object> keyset = Map.of(request.sortBy().getValue(), cursor,
                secondarySortBy, UUID.fromString(idAfter));
        return ScrollPosition.of(keyset, Direction.FORWARD);
    }
}
