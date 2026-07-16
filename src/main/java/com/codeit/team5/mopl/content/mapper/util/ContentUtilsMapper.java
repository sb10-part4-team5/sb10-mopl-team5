package com.codeit.team5.mopl.content.mapper.util;

import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.Named;

@Mapper(componentModel = ComponentModel.SPRING)
public interface ContentUtilsMapper {

    @Named("toTagNames")
    default List<String> toTagNames(Set<ContentTag> contentTags) {
        if (contentTags == null) {
            return Collections.emptyList();
        }
        return contentTags.stream()
                .map(ct -> ct.getTag().getName())
                .sorted()
                .toList();
    }

    @Named("toAverageRating")
    default double toAverageRating(ContentStats stats) {
        return stats == null ? 0.0 : stats.getAverageRating();
    }
}
