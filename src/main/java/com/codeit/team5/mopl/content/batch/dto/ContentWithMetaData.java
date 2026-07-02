package com.codeit.team5.mopl.content.batch.dto;

import com.codeit.team5.mopl.content.entity.Content;
import java.util.List;

public record ContentWithMetaData(
        Content content,
        String thumbnailUrl,
        List<String> tagNames
) {
}
