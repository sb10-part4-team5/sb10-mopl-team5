package com.codeit.team5.mopl.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.mapper.ContentMapperImpl;
import com.codeit.team5.mopl.content.mapper.util.ContentUtilsMapperImpl;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.playlist.dto.PlaylistContentsDto;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.team5.mopl.playlist.dto.response.PlaylistResponse;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.entity.PlaylistItem;
import com.codeit.team5.mopl.playlist.exception.PlaylistAccessDeniedException;
import com.codeit.team5.mopl.playlist.exception.PlaylistContentNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistItemNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistUserNotFoundException;
import com.codeit.team5.mopl.playlist.mapper.PlaylistMapperImpl;
import com.codeit.team5.mopl.playlist.repository.PlaylistItemRepository;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.mapper.UserSummaryMapperImpl;
import com.codeit.team5.mopl.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    private PlaylistService playlistService;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaylistItemRepository playlistItemRepository;

    @Mock
    private ContentRepository contentRepository;

    private User user;
    private Playlist playlist;

    @BeforeEach
    void setUp() {
        ContentUtilsMapperImpl contentUtilsMapper = new ContentUtilsMapperImpl();
        ContentMapperImpl contentMapper = new ContentMapperImpl();
        ReflectionTestUtils.setField(contentMapper, "contentUtilsMapper", contentUtilsMapper);
        UserSummaryMapperImpl userSummaryMapper = new UserSummaryMapperImpl();
        PlaylistMapperImpl playlistMapper =
                new PlaylistMapperImpl(userSummaryMapper, contentMapper);

        playlistService = new PlaylistService(playlistMapper, playlistRepository, userRepository,
                playlistItemRepository, contentRepository);

        user = User.create("test@example.com", "password", "Test User");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        playlist = Playlist.of(user, "My Playlist", "Description");
        ReflectionTestUtils.setField(playlist, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("플레이리스트 생성 성공")
    void create_success() {
        // given
        PlaylistCreateRequest request = new PlaylistCreateRequest("New Title", "New Description");
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

        // when
        PlaylistResponse response = playlistService.create(user.getEmail(), request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("New Title");
        assertThat(response.description()).isEqualTo("New Description");
        
        verify(playlistRepository).save(any(Playlist.class));
    }

    @Test
    @DisplayName("플레이리스트 생성 실패 - 유저를 찾을 수 없음")
    void create_fail_userNotFound() {
        // given
        PlaylistCreateRequest request = new PlaylistCreateRequest("Title", "Desc");
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> playlistService.create("wrong@email.com", request))
                .isInstanceOf(PlaylistUserNotFoundException.class);
    }

    @Test
    @DisplayName("플레이리스트 단건 조회 성공")
    void find_success() {
        // given
        PlaylistContentsDto dto = new PlaylistContentsDto(playlist, Collections.emptyList());
        given(playlistRepository.findByIdWithContents(playlist.getId()))
                .willReturn(Optional.of(dto));

        // when
        PlaylistResponse response = playlistService.find(playlist.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo(playlist.getTitle());
    }

    @Test
    @DisplayName("플레이리스트 단건 조회 실패 - 존재하지 않음")
    void find_fail_notFound() {
        // given
        UUID id = UUID.randomUUID();
        given(playlistRepository.findByIdWithContents(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> playlistService.find(id))
                .isInstanceOf(PlaylistNotFoundException.class);
    }

    @Test
    @DisplayName("플레이리스트 수정 성공")
    void update_success() {
        // given
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("Updated Title", "Updated Desc");
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), user.getEmail()))
                .willReturn(true);
        given(playlistRepository.findById(playlist.getId())).willReturn(Optional.of(playlist));

        // when
        PlaylistResponse response =
                playlistService.update(playlist.getId(), user.getEmail(), request);

        // then
        assertThat(response.title()).isEqualTo("Updated Title");
        assertThat(response.description()).isEqualTo("Updated Desc");
    }

    @Test
    @DisplayName("플레이리스트 수정 실패 - 권한 없음")
    void update_fail_accessDenied() {
        // given
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("Updated Title", "Updated Desc");
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), "other@email.com"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(
                () -> playlistService.update(playlist.getId(), "other@email.com", request))
                        .isInstanceOf(PlaylistAccessDeniedException.class);
    }

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void delete_success() {
        // given
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), user.getEmail()))
                .willReturn(true);

        // when
        playlistService.delete(playlist.getId(), user.getEmail());

        // then
        verify(playlistRepository).deleteByIdDirectly(playlist.getId());
    }

    @Test
    @DisplayName("플레이리스트 삭제 실패 - 권한 없음")
    void delete_fail_accessDenied() {
        // given
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), "other@email.com"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.delete(playlist.getId(), "other@email.com"))
                .isInstanceOf(PlaylistAccessDeniedException.class);
    }

    @Test
    @DisplayName("콘텐츠 추가 성공")
    void addContent_success() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "Content Title", "Desc");
        ReflectionTestUtils.setField(content, "id", contentId);

        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), user.getEmail()))
                .willReturn(true);
        given(contentRepository.existsById(contentId)).willReturn(true);
        given(contentRepository.getReferenceById(contentId)).willReturn(content);

        // when
        playlistService.addContent(user.getEmail(), playlist.getId(), contentId);

        // then
        verify(playlistItemRepository).save(any(PlaylistItem.class));
    }

    @Test
    @DisplayName("콘텐츠 삭제 성공")
    void removeContent_success() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), user.getEmail()))
                .willReturn(true);
        given(contentRepository.existsById(contentId)).willReturn(true);
        given(playlistItemRepository.existsByPlaylistIdAndContentId(playlist.getId(), contentId))
                .willReturn(true);

        // when
        playlistService.removeContent(user.getEmail(), playlist.getId(), contentId);

        // then
        verify(playlistItemRepository).deleteByPlaylistIdAndContentIdDirectly(playlist.getId(),
                contentId);
    }

    @Test
    @DisplayName("콘텐츠 추가 실패 - 권한 없음")
    void addContent_fail_accessDenied() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), "other@email.com"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.addContent("other@email.com", playlist.getId(), contentId))
                .isInstanceOf(PlaylistAccessDeniedException.class);
    }

    @Test
    @DisplayName("콘텐츠 추가 실패 - 콘텐츠를 찾을 수 없음")
    void addContent_fail_contentNotFound() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), user.getEmail()))
                .willReturn(true);
        given(contentRepository.existsById(contentId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.addContent(user.getEmail(), playlist.getId(), contentId))
                .isInstanceOf(PlaylistContentNotFoundException.class);
    }

    @Test
    @DisplayName("콘텐츠 삭제 실패 - 권한 없음")
    void removeContent_fail_accessDenied() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), "other@email.com"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.removeContent("other@email.com", playlist.getId(), contentId))
                .isInstanceOf(PlaylistAccessDeniedException.class);
    }

    @Test
    @DisplayName("콘텐츠 삭제 실패 - 콘텐츠를 찾을 수 없음")
    void removeContent_fail_contentNotFound() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), user.getEmail()))
                .willReturn(true);
        given(contentRepository.existsById(contentId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.removeContent(user.getEmail(), playlist.getId(), contentId))
                .isInstanceOf(PlaylistContentNotFoundException.class);
    }

    @Test
    @DisplayName("콘텐츠 삭제 실패 - 플레이리스트에 해당 콘텐츠가 없음")
    void removeContent_fail_playlistItemNotFound() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerEmail(playlist.getId(), user.getEmail()))
                .willReturn(true);
        given(contentRepository.existsById(contentId)).willReturn(true);
        given(playlistItemRepository.existsByPlaylistIdAndContentId(playlist.getId(), contentId))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.removeContent(user.getEmail(), playlist.getId(), contentId))
                .isInstanceOf(PlaylistItemNotFoundException.class);
    }
}
