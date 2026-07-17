package com.codeit.team5.mopl.watcher.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionContentNotFoundException;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionUserNotFoundException;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@ExecutionTracer(verbose = true)
public class WatchingSessionCommandService {

    private final WatchingSessionRepository repository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void join(UUID contentId, UUID watcherId) {
        if (repository.existsByContentIdAndWatcherId(contentId, watcherId)) {
            return;
        }
        if (!contentRepository.existsById(contentId)) {
            throw new WatchingSessionContentNotFoundException(contentId);
        }
        if (!userRepository.existsById(watcherId)) {
            throw new WatchingSessionUserNotFoundException(watcherId);
        }
        repository.save(new WatchingSession(watcherId, contentId, Instant.now()));
        eventPublisher.publishEvent(new WatcherJoinedEvent(contentId, watcherId));
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void left(UUID contentId, UUID watcherId) {
        if (!repository.existsByContentIdAndWatcherId(contentId, watcherId)) {
            return;
        }
        repository.deleteByContentIdAndWatcherId(contentId, watcherId);
        eventPublisher.publishEvent(new WatcherLeftEvent(contentId));
    }

    @Retryable(maxAttempts = 3,backoff = @Backoff(delay = 1000))
    public void clearContentSessions(UUID contentId) {
        repository.deleteAllByContentId(contentId);
    }
}
