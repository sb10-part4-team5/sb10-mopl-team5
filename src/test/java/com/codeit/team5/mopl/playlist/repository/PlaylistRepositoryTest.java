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

}
