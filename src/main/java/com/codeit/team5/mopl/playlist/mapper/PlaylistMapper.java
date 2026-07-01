package com.codeit.team5.mopl.playlist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.playlist.constant.PlaylistSortBy;
import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCursorRequest;
import com.codeit.team5.mopl.playlist.dto.response.PlaylistResponse;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.user.mapper.UserSummaryMapper;
import jakarta.inject.Named;

@Mapper(config = GlobalMapperConfig.class, uses = {UserSummaryMapper.class})
public interface PlaylistMapper {

    PlaylistResponse toDto(Playlist playlist);

    @Mapping(target = "sortBy", source = "sortBy", qualifiedByName = "toSortBy")
    @Mapping(target = "cursor", source = "cursor", qualifiedByName = "toCursor")
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
}
