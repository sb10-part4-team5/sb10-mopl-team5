package com.codeit.team5.mopl.watcher.service;

import com.codeit.team5.mopl.watcher.event.WatchingSessionCreatedEvent;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.ScrollPosition.Direction;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
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
public class WatchingSessionService {

    private final WatchingSessionRepository repository;
    private final WatchingSessionMapper mapper;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SECONDARY_SORT_FIELD = "id";

    @Transactional
    public WatchingSessionResponse create(UUID contentId, UUID watcherId) {
        User user = userRepository.findWithProfileImageById(watcherId)
                .orElseThrow(() -> new UserNotFoundException(watcherId));
        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        WatchingSession session = WatchingSession.of(user, content);
        repository.save(session);

        eventPublisher.publishEvent(
                new WatchingSessionCreatedEvent(user.getId(), user.getName(), content.getTitle()));

        return mapper.toDto(session);
    }

    public WatchingSessionResponse findSessionByWatchId(UUID watcherId) {
        return repository.findByWatcherId(watcherId).map(mapper::toDto).orElse(null);
    }

    public CursorResponse<WatchingSessionResponse> findSessionByContentId(UUID contentId,
            WatchingSessionCursorRequest request) {
        Sort.Direction direction = request.sortDirection();
        WatcherSortByType sortBy = request.sortBy();
        Sort sort =
                Sort.by(direction, sortBy.getValue()).and(Sort.by(direction, SECONDARY_SORT_FIELD));
        ScrollPosition scrollPosition = createScrollPosition(request);
        Window<WatchingSession> result = repository.findByContentId(contentId, scrollPosition,
                Limit.of(request.limit()), sort);
        Long totalCount = getCurrentWatchingContentView(contentId);
        return mapper.toCursor(result, totalCount, sortBy, direction);
    }

    @Transactional
    public void delete(UUID watcherId) {
        if (repository.existsByWatcherId(watcherId)) {
            repository.deleteByWatcherIdDirectly(watcherId);
            return;
        }
        throw new WatchingSessionNotFoundException(Map.of("watcherId", watcherId));
    }

    public Long getCurrentWatchingContentView(UUID contentId) {
        return repository.countByContentId(contentId);
    }

    public void ensureWatchingContent(UUID watcherId, UUID contentId) {
        if (!repository.existsByWatcherIdAndContentId(watcherId, contentId)) {
            throw new WatchingSessionNotFoundException(
                    Map.of("watcherId", watcherId, "contentId", contentId));
        }
    }


    private ScrollPosition createScrollPosition(WatchingSessionCursorRequest request) {
        String cursor = request.cursor();
        String idAfter = request.idAfter();
        if (cursor == null || idAfter == null) {
            return ScrollPosition.keyset();
        }
        Map<String, Object> keyset = Map.of(request.sortBy().getValue(), cursor,
                SECONDARY_SORT_FIELD, UUID.fromString(idAfter));
        return ScrollPosition.of(keyset, Direction.FORWARD);
    }
}
