package com.codeit.team5.mopl.content.controller;

import com.codeit.team5.mopl.content.controller.api.ContentApi;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> postContent(
            @Valid @RequestPart("request") ContentCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        log.info("Content Create request: POST /api/contents");
        ContentResponse response = contentService.create(request, thumbnail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
