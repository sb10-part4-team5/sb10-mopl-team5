package com.codeit.team5.mopl.playlist.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.entity.PlaylistItem;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;

class PlaylistItemRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentRepository contentRepository;

    private User user;
    private Playlist playlist;
    private Content content;

    @BeforeEach
    void setUp() {
        user = User.create("test@example.com", "password", "Test User");
        userRepository.save(user);

        playlist = Playlist.of(user, "Test Playlist", "Description");
        playlistRepository.save(playlist);

        content = Content.createByAdmin(ContentType.MOVIE, "Movie Title", "Desc");
        contentRepository.save(content);

        flush();
        clear();
        queryInspector.clear();
    }

    @Test
    @DisplayName("플레이리스트 아이템 직접 삭제 및 존재 확인")
    void playlistItemOperations() {
        // given
        PlaylistItem item = PlaylistItem.of(playlist.getId(), content);
        playlistItemRepository.save(item);
        
        flush();
        clear();
        queryInspector.clear();

        // when
        boolean exists = playlistItemRepository.existsByPlaylistIdAndContentId(playlist.getId(), content.getId());
        ensureQueryCount(1);
        
        // then
        assertThat(exists).isTrue();

        // when
        queryInspector.clear();
        playlistItemRepository.deleteByPlaylistIdAndContentIdDirectly(playlist.getId(), content.getId());
        flush();
        clear();

        // then
        queryInspector.clear();
        boolean existsAfterDelete = playlistItemRepository.existsByPlaylistIdAndContentId(playlist.getId(), content.getId());
        ensureQueryCount(1);
        assertThat(existsAfterDelete).isFalse();
    }
}
