package com.codeit.team5.mopl.watcher.mapper.entity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.mapper.util.ContentUtilsMapper;
import com.codeit.team5.mopl.global.mapper.GlobalMapperConfig;
import com.codeit.team5.mopl.watcher.dto.response.WatchingContentResponse;

@Mapper(config = GlobalMapperConfig.class, uses = {ContentUtilsMapper.class})
public interface WatcherContentMapper {

    @Mapping(target = "thumbnailUrl", source = "thumbnail.url")
    @Mapping(target = "reviewCount", source = "stats.reviewCount")
    @Mapping(target = "averageRating", source = "stats", qualifiedByName = "toAverageRating")
    @Mapping(target = "tags", source = "contentTags", qualifiedByName = "toTagNames")
    WatchingContentResponse toDto(Content content);
}
