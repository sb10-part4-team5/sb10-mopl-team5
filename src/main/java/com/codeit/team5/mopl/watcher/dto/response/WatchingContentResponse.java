package com.codeit.team5.mopl.watcher.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingContentResponse(UUID id,
                                      String type,
                                      String title,
                                      String description,
                                      String thumbnailUrl,
                                      List<String> tags,
                                      double averageRating,
                                      int reviewCount) {

}
