package com.codeit.team5.mopl.content.controller.api;

import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.global.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "콘텐츠 관리", description = "콘텐츠 관련 API")
public interface ContentApi {

    @Schema(name = "ContentCreateMultipartRequest")
    class ContentCreateMultipartRequest {
        @Schema(implementation = ContentCreateRequest.class)
        public ContentCreateRequest request;

        @Schema(type = "string", format = "binary", description = "썸네일 이미지")
        public MultipartFile thumbnail;
    }

    @Operation(summary = "[어드민] 콘텐츠 생성", description = "새로운 콘텐츠를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "성공",
                    content = @Content(schema = @Schema(implementation = ContentResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = ContentCreateMultipartRequest.class),
            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
    ))
    ResponseEntity<ContentResponse> postContent(
            @Parameter(hidden = true) ContentCreateRequest request,
            @Parameter(hidden = true) MultipartFile thumbnail
    );

    @Schema(name = "ContentUpdateMultipartRequest")
    class ContentUpdateMultipartRequest {
        @Schema(implementation = ContentUpdateRequest.class)
        public ContentCreateRequest request;

        @Schema(type = "string", format = "binary", description = "썸네일 이미지")
        public MultipartFile thumbnail;
    }

    @Operation(summary = "[어드민] 콘텐츠 수정", description = "콘텐츠의 제목, 설명, 태그를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = ContentResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "콘텐츠 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = ContentUpdateMultipartRequest.class),
            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
    ))
    ResponseEntity<ContentResponse> patchContent(
            @Parameter(description = "콘텐츠 ID") UUID contentId,
            @Parameter(hidden = true) ContentUpdateRequest request,
            @Parameter(hidden = true) MultipartFile thumbnail
    );
}