package com.codeit.team5.mopl.playlist.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;

class PlaylistRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Playlist playlist;

    @BeforeEach
    void setUp() {
        user = User.create("test@example.com", "password", "Test User");
        userRepository.save(user);

        playlist = Playlist.of(user, "Test Playlist", "Description");
        playlistRepository.save(playlist);

        flush();
        clear();
        queryInspector.clear();
    }

    @Test
    @DisplayName("이메일과 아이디로 플레이리스트 소유 여부 확인")
    void existsByIdAndOwnerEmail() {
        // when
        boolean exists = playlistRepository.existsByIdAndOwnerId(playlist.getId(), user.getId());

        // then
        assertThat(exists).isTrue();
        ensureQueryCount(1);

        queryInspector.clear();
        boolean notExists = playlistRepository.existsByIdAndOwnerId(playlist.getId(), UUID.randomUUID());
        ensureQueryCount(1);

        // then
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("직접 플레이리스트 삭제")
    void deleteByIdDirectly() {
        // when
        playlistRepository.deleteByIdDirectly(playlist.getId());
        flush();
        clear();

        // then
        Optional<Playlist> found = playlistRepository.findById(playlist.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("특정 유저가 구독한 모든 플레이리스트의 구독자 수를 1 감소시킨다")
    void bulkDecreaseSubscribeCountBySubscriberId() {
        // given
        User subscriber = persistAndFlush(User.create("sub@test.com", "pass", "sub"));
        persistAndFlush(com.codeit.team5.mopl.subscription.entity.Subscription.of(playlist, subscriber));
        
        // 초기 구독자 수 세팅 (업데이트 쿼리 테스트를 위해 1로 시작)
        entityManager.getEntityManager()
                .createQuery("UPDATE Playlist p SET p.subscriberCount = 1 WHERE p.id = :id")
                .setParameter("id", playlist.getId())
                .executeUpdate();
        clear();

        // when
        playlistRepository.bulkDecreaseSubscribeCountBySubscriberId(subscriber.getId());
        flush();
        clear();

        // then
        Playlist found = playlistRepository.findById(playlist.getId()).orElseThrow();
        assertThat(found.getSubscriberCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("특정 유저가 구독한 모든 플레이리스트의 구독자 수를 1 증가시킨다")
    void bulkIncreaseSubscribeCountBySubscriberId() {
        // given
        User subscriber = persistAndFlush(User.create("sub2@test.com", "pass", "sub"));
        persistAndFlush(com.codeit.team5.mopl.subscription.entity.Subscription.of(playlist, subscriber));
        
        // 초기 구독자 수 세팅
        entityManager.getEntityManager()
                .createQuery("UPDATE Playlist p SET p.subscriberCount = 1 WHERE p.id = :id")
                .setParameter("id", playlist.getId())
                .executeUpdate();
        clear();

        // when
        playlistRepository.bulkIncreaseSubscribeCountBySubscriberId(subscriber.getId());
        flush();
        clear();

        // then
        Playlist found = playlistRepository.findById(playlist.getId()).orElseThrow();
        assertThat(found.getSubscriberCount()).isEqualTo(2);
    }
}
