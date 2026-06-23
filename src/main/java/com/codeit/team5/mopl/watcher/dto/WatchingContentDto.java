package com.codeit.team5.mopl.watcher.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingContentDto(UUID id,
                                 String type,
                                 String title,
                                 String description,
                                 String thumbnailUrl,
                                 List<String> tags,
                                 double averageRating,
                                 int reviewCount) {

}
