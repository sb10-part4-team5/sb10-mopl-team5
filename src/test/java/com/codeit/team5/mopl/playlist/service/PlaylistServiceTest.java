package com.codeit.team5.mopl.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.util.Collections;
import java.util.List;
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
import com.codeit.team5.mopl.playlist.event.PlaylistContentAddEvent;
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
import com.codeit.team5.mopl.subscription.repository.SubscriptionRepository;
import com.codeit.team5.mopl.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;

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

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
                playlistItemRepository, contentRepository, subscriptionRepository, eventPublisher);

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
        given(userRepository.findWithProfileImageById(user.getId())).willReturn(Optional.of(user));

        // when
        PlaylistResponse response = playlistService.create(user.getId(), request);

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
        given(userRepository.findWithProfileImageById(org.mockito.ArgumentMatchers.any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> playlistService.create(UUID.randomUUID(), request))
                .isInstanceOf(PlaylistUserNotFoundException.class);
    }

    @Test
    @DisplayName("플레이리스트 단건 조회 성공")
    void find_success() {
        // given
        PlaylistContentsDto dto = new PlaylistContentsDto(playlist, Collections.emptyList(), false);
        given(playlistRepository.findByIdWithContents(playlist.getId(), user.getId()))
                .willReturn(Optional.of(dto));

        // when
        PlaylistResponse response = playlistService.find(playlist.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo(playlist.getTitle());
    }

    @Test
    @DisplayName("플레이리스트 단건 조회 성공 - 콘텐츠 목록 포함")
    void find_success_with_contents() {
        // given
        Content content = Content.createByAdmin(com.codeit.team5.mopl.content.entity.ContentType.MOVIE, "Movie Title", "Desc");
        PlaylistContentsDto dto = new PlaylistContentsDto(playlist, List.of(content), false);
        given(playlistRepository.findByIdWithContents(playlist.getId(), user.getId()))
                .willReturn(Optional.of(dto));

        // when
        PlaylistResponse response = playlistService.find(playlist.getId(), user.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo(playlist.getTitle());
        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).title()).isEqualTo("Movie Title");
    }

    @Test
    @DisplayName("플레이리스트 단건 조회 실패 - 존재하지 않음")
    void find_fail_notFound() {
        // given
        UUID id = UUID.randomUUID();
        given(playlistRepository.findByIdWithContents(id, user.getId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> playlistService.find(id, user.getId()))
                .isInstanceOf(PlaylistNotFoundException.class);
    }

    @Test
    @DisplayName("플레이리스트 수정 성공")
    void update_success() {
        // given
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("Updated Title", "Updated Desc");
        given(playlistRepository.existsByIdAndOwnerId(playlist.getId(), user.getId()))
                .willReturn(true);
        given(playlistRepository.findByIdWithContents(playlist.getId(), user.getId())).willReturn(Optional.of(new com.codeit.team5.mopl.playlist.dto.PlaylistContentsDto(playlist, java.util.Collections.emptyList(), false)));

        // when
        PlaylistResponse response =
                playlistService.update(playlist.getId(), user.getId(), request);

        // then
        assertThat(response.title()).isEqualTo("Updated Title");
        assertThat(response.description()).isEqualTo("Updated Desc");
    }

    @Test
    @DisplayName("플레이리스트 수정 실패 - 권한 없음")
    void update_fail_accessDenied() {
        // given
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("Updated Title", "Updated Desc");
        given(playlistRepository.existsByIdAndOwnerId(org.mockito.ArgumentMatchers.eq(playlist.getId()), org.mockito.ArgumentMatchers.any(UUID.class)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(
                () -> playlistService.update(playlist.getId(), UUID.randomUUID(), request))
                        .isInstanceOf(PlaylistAccessDeniedException.class);
    }

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void delete_success() {
        // given
        given(playlistRepository.existsByIdAndOwnerId(playlist.getId(), user.getId()))
                .willReturn(true);

        // when
        playlistService.delete(playlist.getId(), user.getId());

        // then
        verify(playlistRepository).deleteByIdDirectly(playlist.getId());
    }

    @Test
    @DisplayName("플레이리스트 삭제 실패 - 권한 없음")
    void delete_fail_accessDenied() {
        // given
        given(playlistRepository.existsByIdAndOwnerId(org.mockito.ArgumentMatchers.eq(playlist.getId()), org.mockito.ArgumentMatchers.any(UUID.class)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.delete(playlist.getId(), UUID.randomUUID()))
                .isInstanceOf(PlaylistAccessDeniedException.class);
    }

    @Test
    @DisplayName("콘텐츠 추가 성공")
    void addContent_success() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.createByAdmin(ContentType.MOVIE, "Content Title", "Desc");
        ReflectionTestUtils.setField(content, "id", contentId);
        UUID subscriberId = UUID.randomUUID();

        given(playlistRepository.existsByIdAndOwnerId(playlist.getId(), user.getId()))
                .willReturn(true);
        given(contentRepository.existsById(contentId)).willReturn(true);
        given(contentRepository.getReferenceById(contentId)).willReturn(content);
        given(playlistRepository.getReferenceById(playlist.getId())).willReturn(playlist);
        given(subscriptionRepository.findSubscriberIdsByPlaylistId(playlist.getId()))
                .willReturn(List.of(subscriberId));

        // when
        playlistService.addContent(user.getId(), playlist.getId(), contentId);

        // then
        verify(playlistItemRepository).save(any(PlaylistItem.class));
        verify(eventPublisher).publishEvent(any(PlaylistContentAddEvent.class));
    }

    @Test
    @DisplayName("콘텐츠 삭제 성공")
    void removeContent_success() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerId(playlist.getId(), user.getId()))
                .willReturn(true);
        given(playlistItemRepository.existsByPlaylistIdAndContentId(playlist.getId(), contentId))
                .willReturn(true);

        // when
        playlistService.removeContent(user.getId(), playlist.getId(), contentId);

        // then
        verify(playlistItemRepository).deleteByPlaylistIdAndContentIdDirectly(playlist.getId(),
                contentId);
    }

    @Test
    @DisplayName("콘텐츠 추가 실패 - 권한 없음")
    void addContent_fail_accessDenied() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerId(org.mockito.ArgumentMatchers.eq(playlist.getId()), org.mockito.ArgumentMatchers.any(UUID.class)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.addContent(UUID.randomUUID(), playlist.getId(), contentId))
                .isInstanceOf(PlaylistAccessDeniedException.class);
    }

    @Test
    @DisplayName("콘텐츠 추가 실패 - 콘텐츠를 찾을 수 없음")
    void addContent_fail_contentNotFound() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerId(playlist.getId(), user.getId()))
                .willReturn(true);
        given(contentRepository.existsById(contentId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.addContent(user.getId(), playlist.getId(), contentId))
                .isInstanceOf(PlaylistContentNotFoundException.class);
    }

    @Test
    @DisplayName("콘텐츠 삭제 실패 - 권한 없음")
    void removeContent_fail_accessDenied() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerId(org.mockito.ArgumentMatchers.eq(playlist.getId()), org.mockito.ArgumentMatchers.any(UUID.class)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.removeContent(UUID.randomUUID(), playlist.getId(), contentId))
                .isInstanceOf(PlaylistAccessDeniedException.class);
    }


    @Test
    @DisplayName("콘텐츠 삭제 실패 - 플레이리스트에 해당 콘텐츠가 없음")
    void removeContent_fail_playlistItemNotFound() {
        // given
        UUID contentId = UUID.randomUUID();
        given(playlistRepository.existsByIdAndOwnerId(playlist.getId(), user.getId()))
                .willReturn(true);
        given(playlistItemRepository.existsByPlaylistIdAndContentId(playlist.getId(), contentId))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.removeContent(user.getId(), playlist.getId(), contentId))
                .isInstanceOf(PlaylistItemNotFoundException.class);
    }
}
