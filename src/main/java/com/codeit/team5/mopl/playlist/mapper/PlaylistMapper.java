package com.codeit.team5.mopl.playlist.mapper;

import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.playlist.dto.response.PlaylistResponse;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.user.mapper.UserSummaryMapper;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class, uses = {UserSummaryMapper.class})
public interface PlaylistMapper {

    PlaylistResponse toDto(Playlist playlist);
}
