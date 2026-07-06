package com.codeit.team5.mopl.subscription.service;

import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.event.PlaylistSubscribedEvent;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.subscription.entity.Subscription;
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
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void create(UUID playlistId, String email) {
        if (!playlistRepository.existsById(playlistId)) {
            throw new SubscriptionPlaylistNotFoundException(playlistId);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SubscriptionUserNotFoundException(email));
        if (repository.existsBySubscriberEmailAndPlaylistId(email, playlistId)) {
            throw new SubscriptionAlreadyExistsException(email, playlistId);
        }
        Playlist playlist = playlistRepository.getReferenceById(playlistId);
        repository.save(Subscription.of(playlist, user));
        eventPublisher.publishEvent(new PlaylistSubscribedEvent(playlist.getOwner().getId(), user.getName(), playlist.getTitle()));
        playlistRepository.increaseSubscribeCount(playlistId);
    }

    @Transactional
    public void delete(UUID playlistId, String email) {
        if (repository.existsBySubscriberEmailAndPlaylistId(email, playlistId)) {
            repository.deleteBySubscriberEmailAndPlaylistIdDirectly(email, playlistId);
            playlistRepository.decreaseSubscribeCount(playlistId);
            return;
        }
        throw new SubscriptionNotFoundException(playlistId, email);
    }
}
