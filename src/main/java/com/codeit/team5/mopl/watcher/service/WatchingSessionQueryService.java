package com.codeit.team5.mopl.watcher.service;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.request.WatchingSessionCursorRequest;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionContentNotFoundException;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionUserNotFoundException;
import com.codeit.team5.mopl.watcher.mapper.entity.WatchingSessionMapper;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Range;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ExecutionTracer
public class WatchingSessionQueryService {

    private final WatchingSessionRepository repository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final WatchingSessionMapper mapper;

    public WatchingSessionResponse findByWatcherId(UUID watcherId) {
        WatchingSession session = repository.findByWatcherId(watcherId).orElse(null);
        if (session == null) {
            return null;
        }
        User user = userRepository.findWithProfileImageById(session.watcherId())
                .orElseThrow(() -> new WatchingSessionUserNotFoundException(session.watcherId()));
        Content content = contentRepository.findWithStatsAndTagsById(session.contentId())
                .orElseThrow(() -> new WatchingSessionContentNotFoundException(session.contentId()));
        return mapper.toDto(session, user, content);
    }

    public CursorResponse<WatchingSessionResponse> findCursorByContentId(UUID contentId,
            WatchingSessionCursorRequest request) {

        Range<Double> scoreRange = request.cursor() != null
                ? Range.rightOpen(0.0, (double) request.cursor().toEpochMilli())
                : Range.closed(0.0, Double.MAX_VALUE);
        int fetchLimit = request.limit() + 1;

        List<WatchingSession> sessions = repository.findWatchingSessionsByContentId(contentId,
                fetchLimit, scoreRange);

        boolean hasNext = sessions.size() > request.limit();
        List<WatchingSession> resultSessions =
                hasNext ? sessions.subList(0, request.limit()) : sessions;
        Map<UUID, WatchingSession> sessionMap = resultSessions.stream()
                .collect(Collectors.toMap(WatchingSession::watcherId, w -> w));

        Map<UUID, User> userMap = userRepository.findWithProfileImageByIdIn(sessionMap.keySet())
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(() -> new WatchingSessionContentNotFoundException(contentId));

        List<WatchingSessionResponse> data = resultSessions.stream()
                .filter(session -> userMap.containsKey(session.watcherId()))
                .map(session -> mapper.toDto(session, userMap.get(session.watcherId()), content))
                .toList();
        Long totalCount = repository.countByContentId(contentId);
        return mapper.toCursor(data, hasNext, totalCount, request.sortBy(),
                request.sortDirection().toString());
    }

    public void ensureWatchingContent(UUID contentId, UUID watcherId) {
        if (!repository.existsByContentIdAndWatcherId(contentId, watcherId)) {
            throw new WatchingSessionNotFoundException(
                    Map.of("watcherId", watcherId, "contentId", contentId));
        }
    }

    public WatchingSessionPayload getWatchingSessionPayload(UUID contentId, UUID watcherId, WatcherStatus status) {
        WatchingSession session = repository.findByWatcherId(watcherId).orElseThrow(
                () -> new WatchingSessionNotFoundException(Map.of("watcherId", watcherId)));

        if (!session.contentId().equals(contentId)) {
            throw new WatchingSessionNotFoundException(
                    Map.of("watcherId", watcherId, "contentId", contentId));
        }

        WatchingSessionResponse response = findByWatcherId(watcherId);
        long watcherCount = repository.countByContentId(session.contentId());
        
        if (status == WatcherStatus.LEAVE) {
            watcherCount = Math.max(0, watcherCount - 1);
        }

        return new WatchingSessionPayload(status, response, watcherCount);
    }
}
