package com.codeit.team5.mopl.content.controller;

import com.codeit.team5.mopl.content.controller.api.ContentApi;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.service.ContentService;
import com.codeit.team5.mopl.binarycontent.support.MultipartFiles;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
@Slf4j
public class ContentController implements ContentApi {

    private final ContentService contentService;

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> postContent(
            @Valid @RequestPart("request") ContentCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        log.info("Content Create request: POST /api/contents");
        ContentResponse response = contentService.create(request, MultipartFiles.toImageResource(thumbnail));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PatchMapping(value = "/{contentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> patchContent(
            @PathVariable UUID contentId,
            @Valid @RequestPart("request") ContentUpdateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        log.info("Content Update request: PATCH /api/contents/{}", contentId);
        ContentResponse response = contentService.update(contentId, request, thumbnail);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId) {
        log.info("Content Delete request: DELETE /api/contents/{}", contentId);
        contentService.delete(contentId);
        return ResponseEntity.noContent().build();
    }
}
