package com.codeit.team5.mopl.subscription.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.subscription.entity.Subscription;
import com.codeit.team5.mopl.subscription.exception.SubscriptionAlreadyExistsException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionPlaylistNotFoundException;
import com.codeit.team5.mopl.subscription.exception.SubscriptionUserNotFoundException;
import com.codeit.team5.mopl.subscription.repository.SubscriptionRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User user;
    private Playlist playlist;

    @BeforeEach
    void setUp() {
        subscriptionService =
                new SubscriptionService(subscriptionRepository, userRepository, playlistRepository, eventPublisher);

        user = User.create("test@example.com", "password", "Test User");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        playlist = Playlist.of(user, "Test Playlist", "Description");
        ReflectionTestUtils.setField(playlist, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("구독 생성 성공")
    void create_success() {
        // given
        UUID playlistId = playlist.getId();
        UUID userId = user.getId();

        given(playlistRepository.existsById(playlistId)).willReturn(true);
        given(userRepository.existsById(userId)).willReturn(true);
        given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(userId, playlistId))
                .willReturn(false);
        given(playlistRepository.getReferenceById(playlistId)).willReturn(playlist);
        given(userRepository.getReferenceById(userId)).willReturn(user);

        // when
        subscriptionService.create(playlistId, userId);

        // then
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("구독 생성 실패 - 플레이리스트를 찾을 수 없음")
    void create_fail_playlistNotFound() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = user.getId();

        given(playlistRepository.existsById(playlistId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> subscriptionService.create(playlistId, userId))
                .isInstanceOf(SubscriptionPlaylistNotFoundException.class);
    }

    @Test
    @DisplayName("구독 생성 실패 - 유저를 찾을 수 없음")
    void create_fail_userNotFound() {
        // given
        UUID playlistId = playlist.getId();
        UUID userId = UUID.randomUUID();

        given(playlistRepository.existsById(playlistId)).willReturn(true);
        given(userRepository.existsById(userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> subscriptionService.create(playlistId, userId))
                .isInstanceOf(SubscriptionUserNotFoundException.class);
    }

    @Test
    @DisplayName("구독 생성 실패 - 이미 구독 중")
    void create_fail_alreadyExists() {
        // given
        UUID playlistId = playlist.getId();
        UUID userId = user.getId();

        given(playlistRepository.existsById(playlistId)).willReturn(true);
        given(userRepository.existsById(userId)).willReturn(true);
        given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(userId, playlistId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> subscriptionService.create(playlistId, userId))
                .isInstanceOf(SubscriptionAlreadyExistsException.class);
    }

    @Test
    @DisplayName("구독 취소(삭제) 성공")
    void delete_success() {
        // given
        UUID playlistId = playlist.getId();
        UUID userId = user.getId();

        given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(userId, playlistId))
                .willReturn(true);

        // when
        subscriptionService.delete(playlistId, userId);

        // then
        verify(subscriptionRepository).deleteBySubscriberIdAndPlaylistIdDirectly(userId,
                playlistId);
    }

    @Test
    @DisplayName("구독 취소 실패 - 구독 내역을 찾을 수 없음")
    void delete_fail_notFound() {
        // given
        UUID playlistId = playlist.getId();
        UUID userId = user.getId();

        given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(userId, playlistId))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> subscriptionService.delete(playlistId, userId))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }
}
