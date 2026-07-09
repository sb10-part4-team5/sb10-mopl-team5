package com.codeit.team5.mopl.content.controller;

import com.codeit.team5.mopl.binarycontent.support.MultipartFiles;
import com.codeit.team5.mopl.content.controller.api.ContentApi;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.facade.ContentFacade;
import com.codeit.team5.mopl.content.service.ContentService;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final ContentFacade contentFacade;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> postContent(
            @Valid @RequestPart("request") ContentCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        ContentResponse response = contentFacade.create(request, MultipartFiles.toImageResource(thumbnail));
        log.info("Content created: id={}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{contentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> patchContent(
            @PathVariable UUID contentId,
            @Valid @RequestPart("request") ContentUpdateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        ContentResponse response = contentFacade.update(contentId, request, MultipartFiles.toImageResource(thumbnail));
        log.info("Content updated: id={}", contentId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{contentId}")
    public ResponseEntity<ContentResponse> getContent(@PathVariable UUID contentId) {
        ContentResponse response = contentService.findById(contentId);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<CursorResponse<ContentResponse>> getContents(@Valid ContentCursorRequest request) {
        CursorResponse<ContentResponse> response = contentService.findContents(request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId) {
        contentService.delete(contentId);
        log.info("Content deleted: id={}", contentId);
        return ResponseEntity.noContent().build();
    }
}
