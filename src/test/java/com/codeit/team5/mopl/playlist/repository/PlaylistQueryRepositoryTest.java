package com.codeit.team5.mopl.playlist.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import com.codeit.team5.mopl.subscription.entity.Subscription;
import com.codeit.team5.mopl.subscription.repository.SubscriptionRepository;
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

    @Autowired
    private SubscriptionRepository subscriptionRepository;

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
        assertThat(dto.subscribedByMe()).isFalse();

        // when (subscribe)
        subscriptionRepository.save(Subscription.of(playlist, user));
        flush();
        clear();
        queryInspector.clear();

        dtoOpt = playlistQueryRepository.findByIdWithContents(playlist.getId(), user.getId());
        ensureQueryCount(2);

        // then
        assertThat(dtoOpt).isPresent();
        assertThat(dtoOpt.get().subscribedByMe()).isTrue();
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
        List<PlaylistContentsDto> result =
                playlistQueryRepository.findByCursor(command, user.getId());
        ensureQueryCount(2); // 1. playlist fetch, 2. playlistItem + content fetch

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).playlist().getId()).isEqualTo(playlist.getId());
        assertThat(result.get(0).subscribedByMe()).isFalse();

        // when (subscribe)
        subscriptionRepository.save(Subscription.of(playlist, user));
        flush();
        clear();
        queryInspector.clear();

        result = playlistQueryRepository.findByCursor(command, user.getId());
        ensureQueryCount(2);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).subscribedByMe()).isTrue();
    }

    @Test
    @DisplayName("커서 기반 다건 연속 조회 (2 페이지 조회)")
    void findByCursor_consecutive_pagination() {
        // given
        Playlist playlist2 = Playlist.of(user, "Test Playlist 2", "Description 2");
        playlistRepository.save(playlist2);

        Playlist playlist3 = Playlist.of(user, "Test Playlist 3", "Description 3");
        playlistRepository.save(playlist3);

        flush();
        clear();
        queryInspector.clear();

        // 1st page query: limit 2
        PlaylistCursorCommand command1 = PlaylistCursorCommand.builder().limit(2)
                .sortDirection(Sort.Direction.DESC).sortBy(PlaylistSortBy.UPDATED_AT).build();

        // when 1
        // repository fetches limit + 1 items internally to check for next page
        List<PlaylistContentsDto> result1 =
                playlistQueryRepository.findByCursor(command1, user.getId());

        // then 1
        assertThat(result1).hasSize(3);

        Playlist lastPlaylist1 = result1.get(1).playlist();

        // 2nd page query: limit 2, with cursor from last element of page 1
        PlaylistCursorCommand command2 = PlaylistCursorCommand.builder().limit(2)
                .cursor(lastPlaylist1.getUpdatedAt()).idAfter(lastPlaylist1.getId())
                .sortDirection(Sort.Direction.DESC).sortBy(PlaylistSortBy.UPDATED_AT).build();

        // when 2
        List<PlaylistContentsDto> result2 =
                playlistQueryRepository.findByCursor(command2, user.getId());

        // then 2
        assertThat(result2).hasSize(1);

        // Combine results to check all 3 are returned exactly once
        List<UUID> allFetchedIds = new ArrayList<>();
        allFetchedIds.add(result1.get(0).playlist().getId());
        allFetchedIds.add(result1.get(1).playlist().getId());
        allFetchedIds.add(result2.get(0).playlist().getId());

        assertThat(allFetchedIds).containsExactlyInAnyOrder(playlist.getId(), playlist2.getId(),
                playlist3.getId());
        assertThat(result2.get(0).playlist().getId()).isEqualTo(playlist.getId());
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
