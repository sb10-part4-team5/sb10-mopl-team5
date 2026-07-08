package com.codeit.team5.mopl.playlist.mapper;

import java.util.List;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.playlist.constant.PlaylistSortBy;
import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCursorRequest;
import com.codeit.team5.mopl.playlist.dto.response.PlaylistResponse;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.user.mapper.UserSummaryMapper;

import com.codeit.team5.mopl.content.mapper.ContentMapper;
import com.codeit.team5.mopl.playlist.dto.PlaylistContentsDto;

@Mapper(config = GlobalMapperConfig.class, uses = {UserSummaryMapper.class, ContentMapper.class})
public interface PlaylistMapper {

    @Mapping(target = ".", source = "playlist")
    PlaylistResponse toDto(PlaylistContentsDto dto);

    @Mapping(target = "contents", expression = "java(java.util.List.of())")
    @Mapping(target = "subscribedByMe", constant = "false")
    PlaylistResponse toDto(Playlist playlist);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<PlaylistResponse> toDto(List<PlaylistContentsDto> playlists);

    @Mapping(target = "sortBy", source = "sortBy", qualifiedByName = "toSortBy")
    @Mapping(target = "cursor", source = "request", qualifiedByName = "toCursor")
    PlaylistCursorCommand toCommand(PlaylistCursorRequest request);

    @Named("toCursor")
    default Object parseCursor(PlaylistCursorRequest request) {
        if (request.cursor() == null) {
            return null;
        }
        return toSortBy(request.sortBy()).parse(request.cursor());
    }

    @Named("toSortBy")
    default PlaylistSortBy toSortBy(String sortBy) {
        return PlaylistSortBy.from(sortBy);
    }

    default CursorResponse<PlaylistResponse> toCursorResponse(List<PlaylistContentsDto> data,
            PlaylistCursorCommand dto, boolean hasNext, long totalCount) {
        String nextCursor = null;
        String nextIdAfter = null;
        if (!data.isEmpty()) {
            Playlist last = data.get(data.size() - 1).playlist();
            nextIdAfter = last.getId().toString();
            nextCursor = switch (dto.sortBy()) {
                case UPDATED_AT -> last.getUpdatedAt().toString();
                case SUBSCRIBE_COUNT -> String.valueOf(last.getSubscriberCount());
            };
        }

        return CursorResponse.<PlaylistResponse>builder().data(toDto(data)).nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter).hasNext(hasNext).totalCount(totalCount)
                .sortBy(dto.sortBy().getSortByType()).sortDirection(dto.sortDirection().name())
                .build();
    }
}
