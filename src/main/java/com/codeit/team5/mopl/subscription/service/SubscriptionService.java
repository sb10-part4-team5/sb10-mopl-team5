package com.codeit.team5.mopl.subscription.service;

import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.subscription.entity.Subscription;
import com.codeit.team5.mopl.subscription.event.PlaylistSubscribedEvent;
import com.codeit.team5.mopl.subscription.exception.SubscriptionAlreadyExistsException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionPlaylistNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionUserNotFoundException;
import com.codeit.team5.mopl.subscription.repository.SubscriptionRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ExecutionTracer
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void create(UUID playlistId, UUID userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new SubscriptionPlaylistNotFoundException(playlistId));
        if (!userRepository.existsById(userId)) {
            throw new SubscriptionUserNotFoundException(userId);
        }
        if (repository.existsBySubscriberIdAndPlaylistId(userId, playlistId)) {
            throw new SubscriptionAlreadyExistsException(userId, playlistId);
        }
        User user = userRepository.getReferenceById(userId);
        repository.save(Subscription.of(playlist, user));

        UUID ownerId = playlist.getOwner().getId();
        if (!ownerId.equals(user.getId())) {
            eventPublisher.publishEvent(new PlaylistSubscribedEvent(ownerId, user.getName(), playlist.getTitle()));
        }

        playlistRepository.increaseSubscribeCount(playlistId);
        log.info("Subscription created: playlistId={}, userId={}", playlistId, userId);
    }

    @Transactional
    public void delete(UUID playlistId, UUID userId) {
        if (repository.existsBySubscriberIdAndPlaylistId(userId, playlistId)) {
            repository.deleteBySubscriberIdAndPlaylistIdDirectly(userId, playlistId);
            playlistRepository.decreaseSubscribeCount(playlistId);
            log.info("Subscription deleted: playlistId={}, userId={}", playlistId, userId);
            return;
        }
        throw new SubscriptionNotFoundException(playlistId, userId);
    }
}
