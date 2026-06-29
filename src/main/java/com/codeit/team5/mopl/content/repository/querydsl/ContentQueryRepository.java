package com.codeit.team5.mopl.content.repository.querydsl;

import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.entity.Content;
import java.util.List;

public interface ContentQueryRepository {

    List<Content> findContents(ContentCursorRequest request, int fetchLimit);

    long countContents(ContentCursorRequest request);
}
