package com.codeit.team5.mopl.subscription.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

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
    @DisplayName("구독자 이메일과 플레이리스트 아이디로 구독 존재 여부 확인")
    void existsBySubscriberEmailAndPlaylistId() {
        // when
        boolean exists = subscriptionRepository.existsBySubscriberEmailAndPlaylistId(user.getEmail(), playlist.getId());
        ensureQueryCount(1);

        queryInspector.clear();
        boolean notExists = subscriptionRepository.existsBySubscriberEmailAndPlaylistId("wrong@email.com", playlist.getId());
        ensureQueryCount(1);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("구독자 이메일과 플레이리스트 아이디로 구독 삭제 (Query 어노테이션)")
    void deleteBySubscriberEmailAndPlaylistIdDirectly() {
        // when
        subscriptionRepository.deleteBySubscriberEmailAndPlaylistIdDirectly(user.getEmail(), playlist.getId());
        ensureQueryCount(1); // delete 쿼리 발생 확인

        flush();
        clear();
        
        // then
        boolean exists = subscriptionRepository.existsBySubscriberEmailAndPlaylistId(user.getEmail(), playlist.getId());
        assertThat(exists).isFalse();
    }
}
