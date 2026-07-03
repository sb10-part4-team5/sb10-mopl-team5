package com.codeit.team5.mopl.playlist.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.playlist.constant.PlaylistSortBy;
import com.codeit.team5.mopl.playlist.dto.PlaylistContentsDto;
import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.entity.PlaylistItem;
import com.codeit.team5.mopl.playlist.repository.query.PlaylistQueryRepositoryImpl;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;

class PlaylistQueryRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistQueryRepositoryImpl playlistQueryRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentRepository contentRepository;

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
    @DisplayName("콘텐츠와 함께 단건 조회")
    void findByIdWithContents() {
        // given
        Content content = Content.createByAdmin(ContentType.MOVIE, "Movie Title", "Desc");
        contentRepository.save(content);

        PlaylistItem item = PlaylistItem.of(playlist.getId(), content);
        playlistItemRepository.save(item);

        flush();
        clear();
        queryInspector.clear();

        // when
        Optional<PlaylistContentsDto> dtoOpt =
                playlistQueryRepository.findByIdWithContents(playlist.getId(), user.getId());
        ensureQueryCount(2); // 1. playlist fetch, 2. playlistItem + content fetch

        // then
        assertThat(dtoOpt).isPresent();
        PlaylistContentsDto dto = dtoOpt.get();
        assertThat(dto.playlist().getId()).isEqualTo(playlist.getId());
        assertThat(dto.contents()).hasSize(1);
        assertThat(dto.contents().get(0).getTitle()).isEqualTo("Movie Title");
    }

    @Test
    @DisplayName("커서 기반 다건 조회")
    void findByCursor() {
        // given
        PlaylistCursorCommand command = PlaylistCursorCommand.builder().limit(10)
                .sortDirection(Sort.Direction.DESC).sortBy(PlaylistSortBy.UPDATED_AT).build();

        flush();
        clear();
        queryInspector.clear();

        // when
        List<PlaylistContentsDto> result = playlistQueryRepository.findByCursor(command, user.getId());
        ensureQueryCount(2); // 1. playlist fetch, 2. playlistItem + content fetch

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).playlist().getId()).isEqualTo(playlist.getId());
    }

    @Test
    @DisplayName("커서 커맨드 기반 카운트 조회")
    void countByCommand() {
        // given
        PlaylistCursorCommand command = PlaylistCursorCommand.builder().keywordLike("Test").build();

        flush();
        clear();
        queryInspector.clear();

        // when
        long count = playlistQueryRepository.countByCommand(command);
        ensureQueryCount(1);

        // then
        assertThat(count).isEqualTo(1L);
    }
}
