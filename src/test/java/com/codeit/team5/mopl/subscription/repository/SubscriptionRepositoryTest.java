package com.codeit.team5.mopl.subscription.repository;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.subscription.entity.Subscription;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;

class SubscriptionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Playlist playlist;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.create("test@example.com", "password", "Test User");
        userRepository.save(user);

        playlist = Playlist.of(user, "Test Playlist", "Description");
        playlistRepository.save(playlist);

        subscription = Subscription.of(playlist, user);
        subscriptionRepository.save(subscription);

        flush();
        clear();
        queryInspector.clear();
    }

    @Test
    @DisplayName("구독자 아이디와 플레이리스트 아이디로 구독 존재 여부 확인")
    void existsBySubscriberIdAndPlaylistId() {
        // when
        boolean exists = subscriptionRepository
                .existsBySubscriberIdAndPlaylistId(user.getId(), playlist.getId());
        ensureQueryCount(1);

        queryInspector.clear();
        boolean notExists = subscriptionRepository
                .existsBySubscriberIdAndPlaylistId(UUID.randomUUID(), playlist.getId());
        ensureQueryCount(1);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("구독자 아이디와 플레이리스트 아이디로 구독 삭제 (Query 어노테이션)")
    void deleteBySubscriberIdAndPlaylistIdDirectly() {
        // when
        subscriptionRepository.deleteBySubscriberIdAndPlaylistIdDirectly(user.getId(),
                playlist.getId());
        ensureQueryCount(1); // delete 쿼리 발생 확인

        flush();
        clear();

        // then
        boolean exists = subscriptionRepository
                .existsBySubscriberIdAndPlaylistId(user.getId(), playlist.getId());
        assertThat(exists).isFalse();
    }
}
