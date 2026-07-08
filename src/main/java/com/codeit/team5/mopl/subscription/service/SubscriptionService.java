package com.codeit.team5.mopl.subscription.service;

import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.subscription.entity.Subscription;
import com.codeit.team5.mopl.subscription.event.SubscriptionCreatedEvent;
import com.codeit.team5.mopl.subscription.exception.SubscriptionAlreadyExistsException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionPlaylistNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionUserNotFoundException;
import com.codeit.team5.mopl.subscription.repository.SubscriptionRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            eventPublisher.publishEvent(new SubscriptionCreatedEvent(playlistId, userId));
        }

        playlistRepository.increaseSubscribeCount(playlistId);
    }

    @Transactional
    public void delete(UUID playlistId, UUID userId) {
        if (repository.existsBySubscriberIdAndPlaylistId(userId, playlistId)) {
            repository.deleteBySubscriberIdAndPlaylistIdDirectly(userId, playlistId);
            playlistRepository.decreaseSubscribeCount(playlistId);
            return;
        }
        throw new SubscriptionNotFoundException(playlistId, userId);
    }
}
