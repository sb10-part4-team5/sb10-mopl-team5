package com.codeit.team5.mopl.subscription.service;

import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.subscription.entity.Subscription;
import com.codeit.team5.mopl.subscription.exception.SubscriptionNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionPlaylistNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionUserNotFoundException;
import com.codeit.team5.mopl.subscription.repository.SubscriptionRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;

    @Transactional
    public void create(UUID playlistId, String email) {
        if (!playlistRepository.existsById(playlistId)) {
            throw new SubscriptionPlaylistNotFoundException(playlistId);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SubscriptionUserNotFoundException(email));
        Playlist playlist = playlistRepository.getReferenceById(playlistId);
        repository.save(Subscription.of(playlist, user));
    }

    @Transactional
    public void delete(UUID playlistId, String email) {
        if (repository.existsBySubscriberEmailAndPlaylistId(email, playlistId)) {
            repository.deleteBySubscriberEmailAndPlaylistIdDirectly(email, playlistId);
            return;
        }
        throw new SubscriptionNotFoundException(playlistId, email);
    }
}
