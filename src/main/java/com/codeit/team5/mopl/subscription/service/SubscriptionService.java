package com.codeit.team5.mopl.subscription.service;

import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.subscription.event.PlaylistSubscribedEvent;
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
    public void create(UUID playlistId, UUID userId) {
        if (!playlistRepository.existsById(playlistId)) {
            throw new SubscriptionPlaylistNotFoundException(playlistId);
        }
        if (!userRepository.existsById(userId)) {
            throw new SubscriptionUserNotFoundException(userId);
        }
        if (repository.existsBySubscriberIdAndPlaylistId(userId, playlistId)) {
            throw new SubscriptionAlreadyExistsException(userId, playlistId);
        }
        Playlist playlist = playlistRepository.getReferenceById(playlistId);
        User user = userRepository.getReferenceById(userId);
        repository.save(Subscription.of(playlist, user));

        // 세 파라미터가 event를 정의하는데 필요한 최소의 한 쌍이라고 판단해서
        // 놔두었습니다.
        UUID ownerId = playlist.getOwner().getId();
        String subscriberName = user.getName();
        String playlistTitle = playlist.getTitle();

        // 자기 자신의 플레이리스트를 구독하는지 검증하고 이벤트 발행
        if (!ownerId.equals(user.getId())) {
            eventPublisher.publishEvent(                new PlaylistSubscribedEvent(
                ownerId,
                subscriberName,
                playlistTitle
            )
            );
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
