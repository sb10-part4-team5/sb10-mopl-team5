package com.codeit.team5.mopl.content.mapper;

import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.mapper.util.ContentUtilsMapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {ContentUtilsMapper.class})
public interface ContentMapper {

    @Mapping(target = "tags", source = "contentTags", qualifiedByName = "toTagNames")
    @Mapping(target = "averageRating", source = "stats", qualifiedByName = "toAverageRating")
    @Mapping(target = "reviewCount", source = "stats.reviewCount")
    @Mapping(target = "watcherCount", source = "stats.watcherCount")
    @Mapping(target = "thumbnailUrl", source = "thumbnail.url")
    @Mapping(target = "thumbnailUploadStatus", source = "thumbnail.uploadStatus")
    ContentResponse toDto(Content content);
}
