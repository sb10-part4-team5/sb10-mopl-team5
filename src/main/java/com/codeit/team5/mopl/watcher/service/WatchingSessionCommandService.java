package com.codeit.team5.mopl.watcher.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.constant.WatcherStatus;
import com.codeit.team5.mopl.watcher.dto.payload.WatchingSessionPayload;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionAlreadyExistsException;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.mapper.entity.WatchingSessionMapper;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
@ExecutionTracer
public class WatchingSessionCommandService {

    private final WatchingSessionRepository repository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WatchingSessionMapper mapper;

    public WatchingSessionPayload join(UUID contentId, UUID watcherId) {
        if (repository.existsByWatcherId(watcherId)) {
            throw new WatchingSessionAlreadyExistsException(watcherId);
        }
        User user = userRepository.findWithProfileImageById(watcherId)
                .orElseThrow(() -> new UserNotFoundException(watcherId));
        Content content = contentRepository.findWithStatsAndTagsById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        WatchingSession session = WatchingSession.of(user, content);
        repository.save(session);
        eventPublisher.publishEvent(new WatcherJoinedEvent(contentId));
        return new WatchingSessionPayload(WatcherStatus.JOIN, mapper.toDto(session),
                repository.countByContentId(contentId));
    }

    public WatchingSessionPayload left(UUID watcherId) {
        WatchingSession session = repository.findByWatcherId(watcherId).orElseThrow(
                () -> new WatchingSessionNotFoundException(Map.of("watcherId", watcherId)));
        UUID contentId = session.getContent().getId();
        WatchingSessionResponse response = mapper.toDto(session);
        repository.deleteByWatcherIdDirectly(watcherId);
        eventPublisher.publishEvent(new WatcherLeftEvent(contentId));
        return new WatchingSessionPayload(WatcherStatus.LEAVE, response,
                repository.countByContentId(contentId));
    }
}
