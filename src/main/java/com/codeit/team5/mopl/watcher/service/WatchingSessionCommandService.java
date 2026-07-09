package com.codeit.team5.mopl.watcher.service;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;
import com.codeit.team5.mopl.watcher.event.WatcherLeftEvent;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionContentNotFoundException;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionUserNotFoundException;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;

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
        Content content = contentRepository.getReferenceById(contentId);
        User user = userRepository.getReferenceById(watcherId);
        repository.save(WatchingSession.of(user, content));
        eventPublisher.publishEvent(new WatcherJoinedEvent(contentId, watcherId));
    }

    public void left(UUID contentId, UUID watcherId) {
        if (!repository.existsByContentIdAndWatcherId(contentId, watcherId)) {
            return;
        }
        repository.deleteByContentIdAndWatcherIdDirectly(contentId, watcherId);
        eventPublisher.publishEvent(new WatcherLeftEvent(contentId));
    }
}
