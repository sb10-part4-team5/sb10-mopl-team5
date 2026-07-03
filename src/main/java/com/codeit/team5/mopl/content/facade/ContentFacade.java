package com.codeit.team5.mopl.content.facade;

import com.codeit.team5.mopl.binarycontent.service.UploadWithRollback;
import com.codeit.team5.mopl.binarycontent.storage.StorageDirectory;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.service.ContentService;
import com.codeit.team5.mopl.global.dto.FileRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentFacade {

    private final ContentService contentService;
    private final UploadWithRollback uploadWithRollback;

    public ContentResponse create(ContentCreateRequest request, FileRequest image) {
        return uploadWithRollback.execute(StorageDirectory.THUMBNAIL, image,
                uploaded -> contentService.create(request, uploaded));
    }

    public ContentResponse update(UUID contentId, ContentUpdateRequest request, FileRequest image) {
        return uploadWithRollback.execute(StorageDirectory.THUMBNAIL, image,
                uploaded -> contentService.update(contentId, request, uploaded));
    }
}
