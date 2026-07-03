package com.codeit.team5.mopl.playlist.service;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.playlist.dto.PlaylistContentsDto;
import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.team5.mopl.playlist.dto.response.PlaylistResponse;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.entity.PlaylistItem;
import com.codeit.team5.mopl.playlist.exception.PlaylistAccessDeniedException;
import com.codeit.team5.mopl.playlist.exception.PlaylistContentNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistUserNotFoundException;
import com.codeit.team5.mopl.playlist.mapper.PlaylistMapper;
import com.codeit.team5.mopl.playlist.repository.PlaylistItemRepository;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistMapper mapper;
    private final PlaylistRepository repository;
    private final UserRepository userRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final ContentRepository contentRepository;

    @Transactional
    public PlaylistResponse create(String email, PlaylistCreateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PlaylistUserNotFoundException(email));
        Playlist playlist = Playlist.of(user, request.title(), request.description());
        return mapper.toDto(playlist);
    }

    public PlaylistResponse find(UUID id) {
        PlaylistContentsDto dto = repository.findByIdWithContents(id)
                .orElseThrow(() -> new PlaylistNotFoundException(id));
        return mapper.toDto(dto);
    }

    @Transactional
    public PlaylistResponse update(UUID id, String email, PlaylistUpdateRequest request) {
        validateOwner(id, email);
        Playlist playlist = findById(id);
        playlist.updateTitle(request.title());
        playlist.updateDescription(request.description());
        return mapper.toDto(playlist);
    }

    @Transactional
    public void delete(UUID id, String email) {
        validateOwner(id, email);
        repository.deleteByIdDirectly(id);
    }

    public CursorResponse<PlaylistResponse> findByCursor(PlaylistCursorCommand dto) {
        List<PlaylistContentsDto> playlists = repository.findByCursor(dto);
        boolean hasNext = playlists.size() > dto.limit();
        List<PlaylistContentsDto> data = hasNext ? playlists.subList(0, dto.limit()) : playlists;
        long totalCount = repository.countByCommand(dto);
        return mapper.toCursorResponse(data, dto, hasNext, totalCount);
    }

    @Transactional
    public void addContent(String email, UUID playlistId, UUID contentId) {
        validateOwner(playlistId, email);
        if (!contentRepository.existsById(contentId)) {
            throw new PlaylistContentNotFoundException(contentId);
        }
        Content content = contentRepository.getReferenceById(contentId);
        PlaylistItem playlistItem = PlaylistItem.of(playlistId, content);
        playlistItemRepository.save(playlistItem);
    }

    @Transactional
    public void removeContent(String email, UUID playlistId, UUID contentId) {
        validateOwner(playlistId, email);
        if (!contentRepository.existsById(contentId)) {
            throw new PlaylistContentNotFoundException(contentId);
        }
        if (!playlistItemRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) {
            throw new PlaylistItemNotFoundException(playlistId, contentId);
        }
        playlistItemRepository.deleteByPlaylistIdAndContentIdDirectly(playlistId, contentId);
    }

    private Playlist findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PlaylistNotFoundException(id));
    }

    private void validateOwner(UUID id, String email) {
        if (repository.existsByIdAndOwnerEmail(id, email)) {
            return;
        }
        throw new PlaylistAccessDeniedException(id, email);
    }
}
