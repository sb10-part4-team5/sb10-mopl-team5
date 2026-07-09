package com.codeit.team5.mopl.playlist.service;

import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;
import com.codeit.team5.mopl.playlist.event.PlaylistContentAddEvent;
import com.codeit.team5.mopl.playlist.event.PlaylistCreatedEvent;
import com.codeit.team5.mopl.playlist.exception.PlaylistItemAlreadyExistsException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.codeit.team5.mopl.playlist.exception.PlaylistItemNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistNotFoundException;
import com.codeit.team5.mopl.playlist.exception.PlaylistUserNotFoundException;
import com.codeit.team5.mopl.playlist.mapper.PlaylistMapper;
import com.codeit.team5.mopl.playlist.repository.PlaylistItemRepository;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ExecutionTracer
public class PlaylistService {

    private final PlaylistMapper mapper;
    private final PlaylistRepository repository;
    private final UserRepository userRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final ContentRepository contentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PlaylistResponse create(UUID userId, PlaylistCreateRequest request) {
        User user = userRepository.findWithProfileImageById(userId)
                .orElseThrow(() -> new PlaylistUserNotFoundException(userId));
        Playlist playlist = Playlist.of(user, request.title(), request.description());
        repository.saveAndFlush(playlist);
        eventPublisher.publishEvent(new PlaylistCreatedEvent(playlist.getId()));
        log.info("Playlist created: id={}, ownerId={}", playlist.getId(), userId);
        return mapper.toDto(playlist);
    }

    public PlaylistResponse find(UUID id, UUID userId) {
        return mapper.toDto(findById(id, userId));
    }

    @Transactional
    public PlaylistResponse update(UUID id, UUID userId, PlaylistUpdateRequest request) {
        validateOwner(id, userId);
        PlaylistContentsDto dto = findById(id, userId);
        Playlist playlist = dto.playlist();
        playlist.updateTitle(request.title());
        playlist.updateDescription(request.description());
        log.info("Playlist updated: id={}", id);
        return mapper.toDto(dto);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        validateOwner(id, userId);
        repository.deleteByIdDirectly(id);
        log.info("Playlist deleted: id={}", id);
    }

    public CursorResponse<PlaylistResponse> findByCursor(PlaylistCursorCommand dto, UUID userId) {
        List<PlaylistContentsDto> playlists = repository.findByCursor(dto, userId);
        boolean hasNext = playlists.size() > dto.limit();
        List<PlaylistContentsDto> data = hasNext ? playlists.subList(0, dto.limit()) : playlists;
        long totalCount = repository.countByCommand(dto);
        return mapper.toCursorResponse(data, dto, hasNext, totalCount);
    }

    @Transactional
    public void addContent(UUID userId, UUID playlistId, UUID contentId) {
        validateOwner(playlistId, userId);
        if (!contentRepository.existsById(contentId)) {
            throw new PlaylistContentNotFoundException(contentId);
        }
        if (playlistItemRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) {
            throw new PlaylistItemAlreadyExistsException(playlistId, contentId);
        }
        Content content = contentRepository.getReferenceById(contentId);
        Playlist playlist = repository.getReferenceById(playlistId);
        PlaylistItem playlistItem = PlaylistItem.of(playlistId, content);
        playlistItemRepository.save(playlistItem);

        eventPublisher.publishEvent(new PlaylistContentAddEvent(
                playlistId, playlist.getTitle(), content.getTitle()
        ));
        log.info("Playlist content added: playlistId={}, contentId={}", playlistId, contentId);
    }

    @Transactional
    public void removeContent(UUID userId, UUID playlistId, UUID contentId) {
        validateOwner(playlistId, userId);
        if (!playlistItemRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) {
            throw new PlaylistItemNotFoundException(playlistId, contentId);
        }
        playlistItemRepository.deleteByPlaylistIdAndContentIdDirectly(playlistId, contentId);
        log.info("Playlist content removed: playlistId={}, contentId={}", playlistId, contentId);
    }

    private void validateOwner(UUID id, UUID userId) {
        if (repository.existsByIdAndOwnerId(id, userId)) {
            return;
        }
        throw new PlaylistAccessDeniedException(id, userId);
    }

    private PlaylistContentsDto findById(UUID id, UUID userId) {
        PlaylistContentsDto dto = repository.findByIdWithContents(id, userId)
                .orElseThrow(() -> new PlaylistNotFoundException(id));
        return dto;
    }
}
