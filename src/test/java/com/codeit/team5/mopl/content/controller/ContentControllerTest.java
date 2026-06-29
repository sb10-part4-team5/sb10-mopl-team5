package com.codeit.team5.mopl.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.service.ContentService;
import com.codeit.team5.mopl.global.dto.FileRequest;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ContentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestGlobalExceptionHandlerConfig.class})
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContentService contentService;

    @Captor
    private ArgumentCaptor<ContentCreateRequest> requestCaptor;


    // --- POST ---
    @Test
    @DisplayName("정상적인 콘텐츠 생성 요청이면 생성된 콘텐츠와 201 응답을 반환한다")
    void postContent_success() throws Exception {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                "테스트 설명",
                List.of("액션", "드라마")
        );
        ContentResponse response = new ContentResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                ContentType.MOVIE,
                "테스트 영화",
                "테스트 설명",
                "http://localhost:8080/thumbnails/11111111-1111-1111-1111-111111111111/test.jpg",
                BinaryContentUploadStatus.PENDING,
                List.of("액션", "드라마"),
                0.0, 0, 0
        );

        given(contentService.create(any(ContentCreateRequest.class), any())).willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );
        MockMultipartFile thumbnailPart = new MockMultipartFile(
                "thumbnail",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3}
        );

        // When & Then
        mockMvc.perform(multipart("/api/contents")
                        .file(requestPart)
                        .file(thumbnailPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.type").value("movie"))
                .andExpect(jsonPath("$.title").value("테스트 영화"))
                .andExpect(jsonPath("$.description").value("테스트 설명"))
                .andExpect(jsonPath("$.thumbnailUrl").value(response.thumbnailUrl()))
                .andExpect(jsonPath("$.thumbnailUploadStatus").value("PENDING"))
                .andExpect(jsonPath("$.tags[0]").value("액션"))
                .andExpect(jsonPath("$.tags[1]").value("드라마"))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.watcherCount").value(0));

        ArgumentCaptor<FileRequest> thumbnailCaptor = ArgumentCaptor.forClass(FileRequest.class);
        verify(contentService).create(requestCaptor.capture(), thumbnailCaptor.capture());
        assertThat(thumbnailCaptor.getValue().filename()).isEqualTo("test.jpg");
        assertThat(thumbnailCaptor.getValue().bytes()).containsExactly(1, 2, 3);
        ContentCreateRequest captured = requestCaptor.getValue();
        assertThat(captured.type()).isEqualTo(ContentType.MOVIE);
        assertThat(captured.title()).isEqualTo("테스트 영화");
        assertThat(captured.description()).isEqualTo("테스트 설명");
        assertThat(captured.tags()).containsExactly("액션", "드라마");
    }

    @Test
    @DisplayName("thumbnail 없이 콘텐츠 생성 요청이면 201 응답을 반환한다")
    void postContent_withoutThumbnail_success() throws Exception {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.TV_SERIES,
                "테스트 드라마",
                "테스트 설명",
                List.of("로맨스")
        );
        ContentResponse response = new ContentResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                ContentType.TV_SERIES,
                "테스트 드라마",
                "테스트 설명",
                null,
                null,
                List.of("로맨스"),
                0.0, 0, 0
        );

        given(contentService.create(any(ContentCreateRequest.class), isNull())).willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart("/api/contents")
                        .file(requestPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.type").value("tvSeries"))
                .andExpect(jsonPath("$.title").value("테스트 드라마"))
                .andExpect(jsonPath("$.description").value("테스트 설명"))
                .andExpect(jsonPath("$.tags[0]").value("로맨스"))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.watcherCount").value(0));
        verify(contentService).create(requestCaptor.capture(), isNull());
        ContentCreateRequest captured = requestCaptor.getValue();
        assertThat(captured.type()).isEqualTo(ContentType.TV_SERIES);
        assertThat(captured.title()).isEqualTo("테스트 드라마");
        assertThat(captured.description()).isEqualTo("테스트 설명");
        assertThat(captured.tags()).containsExactly("로맨스");
    }

    @Test
    @DisplayName("콘텐츠 타입이 누락되면 400 검증 실패 응답을 반환한다")
    void postContent_missingType_returnsBadRequest() throws Exception {
        // Given
        String requestJson = """
                {
                  "title": "테스트 영화",
                  "tags": ["액션"]
                }
                """;
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/contents")
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.type[0]").value("콘텐츠 타입은 필수입니다."));

        verify(contentService, never()).create(any(), any());
    }

    @Test
    @DisplayName("제목이 공백이면 400 검증 실패 응답을 반환한다")
    void postContent_blankTitle_returnsBadRequest() throws Exception {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "   ",
                null,
                List.of("액션")
        );
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart("/api/contents")
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.title[0]").value("제목은 필수입니다."));

        verify(contentService, never()).create(any(), any());
    }

    @Test
    @DisplayName("태그 목록이 비어있으면 400 검증 실패 응답을 반환한다")
    void postContent_emptyTags_returnsBadRequest() throws Exception {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                null,
                List.of()
        );
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart("/api/contents")
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.tags[0]").value("콘텐츠 태그 목록은 비어있을 수 없습니다."));

        verify(contentService, never()).create(any(), any());
    }

    @Test
    @DisplayName("예상하지 못한 서비스 예외가 발생하면 500 오류 응답을 반환한다")
    void postContent_unexpectedException_returnsInternalServerError() throws Exception {
        // Given
        ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE,
                "테스트 영화",
                null,
                List.of("액션")
        );
        given(contentService.create(any(ContentCreateRequest.class), any()))
                .willThrow(new IllegalStateException("unexpected"));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart("/api/contents")
                        .file(requestPart))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionType").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 에러가 발생했습니다."));
    }

    // --- PATCH ---
    @Test
    @DisplayName("정상적인 콘텐츠 수정 요청이면 수정된 콘텐츠와 200 응답을 반환한다")
    void patchContent_success() throws Exception {
        // Given
        UUID contentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ContentUpdateRequest request = new ContentUpdateRequest(
                "수정된 영화",
                "수정된 설명",
                List.of("액션", "SF")
        );
        ContentResponse response = new ContentResponse(
                contentId,
                ContentType.MOVIE,
                "수정된 영화",
                "수정된 설명",
                "http://localhost:8080/thumbnails/11111111-1111-1111-1111-111111111111/new.jpg",
                BinaryContentUploadStatus.PENDING,
                List.of("SF", "액션"),
                0.0, 0, 0
        );

        given(contentService.update(eq(contentId), any(ContentUpdateRequest.class), any()))
                .willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );
        MockMultipartFile thumbnailPart = new MockMultipartFile(
                "thumbnail", "new.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{4, 5, 6}
        );

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{contentId}", contentId)
                        .file(requestPart)
                        .file(thumbnailPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contentId.toString()))
                .andExpect(jsonPath("$.type").value("movie"))
                .andExpect(jsonPath("$.title").value("수정된 영화"))
                .andExpect(jsonPath("$.description").value("수정된 설명"))
                .andExpect(jsonPath("$.thumbnailUrl").value(response.thumbnailUrl()))
                .andExpect(jsonPath("$.thumbnailUploadStatus").value("PENDING"))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.watcherCount").value(0));

        ArgumentCaptor<ContentUpdateRequest> requestCaptor = ArgumentCaptor.forClass(ContentUpdateRequest.class);
        ArgumentCaptor<FileRequest> thumbnailCaptor = ArgumentCaptor.forClass(FileRequest.class);
        verify(contentService).update(eq(contentId), requestCaptor.capture(), thumbnailCaptor.capture());
        assertThat(requestCaptor.getValue().title()).isEqualTo("수정된 영화");
        assertThat(requestCaptor.getValue().description()).isEqualTo("수정된 설명");
        assertThat(requestCaptor.getValue().tags()).containsExactly("액션", "SF");
        assertThat(thumbnailCaptor.getValue().filename()).isEqualTo("new.jpg");
        assertThat(thumbnailCaptor.getValue().bytes()).containsExactly(4, 5, 6);
    }

    @Test
    @DisplayName("thumbnail 없이 콘텐츠 수정 요청이면 200 응답을 반환한다")
    void patchContent_withoutThumbnail_success() throws Exception {
        // Given
        UUID contentId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        ContentUpdateRequest request = new ContentUpdateRequest(
                "수정된 드라마",
                null,
                List.of("로맨스")
        );
        ContentResponse response = new ContentResponse(
                contentId,
                ContentType.TV_SERIES,
                "수정된 드라마",
                null,
                null,
                null,
                List.of("로맨스"),
                0.0, 0, 0
        );

        given(contentService.update(eq(contentId), any(ContentUpdateRequest.class), isNull()))
                .willReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{contentId}", contentId)
                        .file(requestPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contentId.toString()))
                .andExpect(jsonPath("$.title").value("수정된 드라마"))
                .andExpect(jsonPath("$.tags[0]").value("로맨스"));

        verify(contentService).update(eq(contentId), any(ContentUpdateRequest.class), isNull());
    }

    @Test
    @DisplayName("제목이 공백이면 400 검증 실패 응답을 반환한다")
    void patchContent_blankTitle_returnsBadRequest() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        ContentUpdateRequest request = new ContentUpdateRequest("   ", null, List.of("액션"));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{contentId}", contentId)
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.title[0]").value("제목은 필수입니다."));

        verify(contentService, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("태그가 null이면 400 검증 실패 응답을 반환한다")
    void patchContent_nullTags_returnsBadRequest() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        String requestJson = """
                {
                  "title": "수정된 영화"
                }
                """;
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes()
        );

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{contentId}", contentId)
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.tags", hasItem("콘텐츠 태그는 필수입니다.")));

        verify(contentService, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("태그 목록이 비어있으면 400 검증 실패 응답을 반환한다")
    void patchContent_emptyTags_returnsBadRequest() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        ContentUpdateRequest request = new ContentUpdateRequest("수정된 영화", null, List.of());
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{contentId}", contentId)
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.tags[0]").value("콘텐츠 태그 목록은 비어있을 수 없습니다."));

        verify(contentService, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("태그 항목이 공백이면 400 검증 실패 응답을 반환한다")
    void patchContent_blankTagItem_returnsBadRequest() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        ContentUpdateRequest request = new ContentUpdateRequest("수정된 영화", null, List.of("  "));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{contentId}", contentId)
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details['tags[0]'][0]").value("태그는 공백일 수 없습니다."));

        verify(contentService, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 수정 요청이면 404 응답을 반환한다")
    void patchContent_notFound_returnsNotFound() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        ContentUpdateRequest request = new ContentUpdateRequest("수정된 영화", null, List.of("액션"));
        given(contentService.update(eq(contentId), any(ContentUpdateRequest.class), any()))
                .willThrow(new ContentNotFoundException(contentId));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{contentId}", contentId)
                        .file(requestPart))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("ContentNotFoundException"))
                .andExpect(jsonPath("$.message").value("콘텐츠를 찾을 수 없습니다."));

        verify(contentService).update(eq(contentId), any(ContentUpdateRequest.class), any());
    }

    @Test
    @DisplayName("예상하지 못한 서비스 예외가 발생하면 500 오류 응답을 반환한다")
    void patchContent_unexpectedException_returnsInternalServerError() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        ContentUpdateRequest request = new ContentUpdateRequest("수정된 영화", null, List.of("액션"));
        given(contentService.update(eq(contentId), any(ContentUpdateRequest.class), any()))
                .willThrow(new IllegalStateException("unexpected"));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{contentId}", contentId)
                        .file(requestPart))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionType").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 에러가 발생했습니다."));
    }

    // --- GET (단건) ---
    @Test
    @DisplayName("존재하는 콘텐츠 ID로 조회하면 콘텐츠와 200 응답을 반환한다")
    void getContent_success() throws Exception {
        // Given
        UUID contentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ContentResponse response = new ContentResponse(
                contentId,
                ContentType.MOVIE,
                "테스트 영화",
                "테스트 설명",
                "http://localhost:8080/thumbnails/test.jpg",
                BinaryContentUploadStatus.PENDING,
                List.of("액션", "SF"),
                4.5, 10, 100L
        );

        given(contentService.findById(contentId)).willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/contents/{contentId}", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contentId.toString()))
                .andExpect(jsonPath("$.type").value("movie"))
                .andExpect(jsonPath("$.title").value("테스트 영화"))
                .andExpect(jsonPath("$.description").value("테스트 설명"))
                .andExpect(jsonPath("$.thumbnailUrl").value(response.thumbnailUrl()))
                .andExpect(jsonPath("$.thumbnailUploadStatus").value("PENDING"))
                .andExpect(jsonPath("$.tags[0]").value("액션"))
                .andExpect(jsonPath("$.tags[1]").value("SF"))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(10))
                .andExpect(jsonPath("$.watcherCount").value(100));

        verify(contentService).findById(contentId);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 ID로 조회하면 404 응답을 반환한다")
    void getContent_notFound_returnsNotFound() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        given(contentService.findById(contentId)).willThrow(new ContentNotFoundException(contentId));

        // When & Then
        mockMvc.perform(get("/api/contents/{contentId}", contentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("ContentNotFoundException"))
                .andExpect(jsonPath("$.message").value("콘텐츠를 찾을 수 없습니다."));

        verify(contentService).findById(contentId);
    }

    @Test
    @DisplayName("단건 조회 중 예상하지 못한 예외가 발생하면 500 응답을 반환한다")
    void getContent_unexpectedException_returnsInternalServerError() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        given(contentService.findById(contentId)).willThrow(new IllegalStateException("unexpected"));

        // When & Then
        mockMvc.perform(get("/api/contents/{contentId}", contentId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionType").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 에러가 발생했습니다."));
    }

    // --- GET (목록) ---
    @Test
    @DisplayName("필수 파라미터만으로 콘텐츠 목록을 조회하면 200 응답을 반환한다")
    void getContents_success() throws Exception {
        // Given
        ContentResponse item = new ContentResponse(
                UUID.randomUUID(),
                ContentType.MOVIE,
                "테스트 영화",
                "테스트 설명",
                null, null,
                List.of("액션"),
                0.0, 0, 0L
        );
        CursorResponse<ContentResponse> response = new CursorResponse<>(
                List.of(item),
                null, null, false, 1L,
                "createdAt", "DESCENDING"
        );

        given(contentService.findContents(any(ContentCursorRequest.class))).willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/contents")
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(item.id().toString()))
                .andExpect(jsonPath("$.data[0].title").value("테스트 영화"))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.sortBy").value("createdAt"))
                .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));

        verify(contentService).findContents(any(ContentCursorRequest.class));
    }

    @Test
    @DisplayName("모든 필터 파라미터를 포함한 콘텐츠 목록 조회 시 200 응답을 반환한다")
    void getContents_withAllFilters_success() throws Exception {
        // Given
        CursorResponse<ContentResponse> response = new CursorResponse<>(
                List.of(), null, null, false, 0L, "createdAt", "DESCENDING"
        );

        given(contentService.findContents(any(ContentCursorRequest.class))).willReturn(response);

        ArgumentCaptor<ContentCursorRequest> requestCaptor =
                ArgumentCaptor.forClass(ContentCursorRequest.class);

        // When & Then
        mockMvc.perform(get("/api/contents")
                        .param("typeEqual", "movie")
                        .param("keywordLike", "어벤져스")
                        .param("tagsIn", "액션", "SF")
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isOk());

        verify(contentService).findContents(requestCaptor.capture());
        ContentCursorRequest captured = requestCaptor.getValue();
        assertThat(captured.typeEqual()).isEqualTo(ContentType.MOVIE);
        assertThat(captured.keywordLike()).isEqualTo("어벤져스");
        assertThat(captured.tagsIn()).containsExactly("액션", "SF");
        assertThat(captured.limit()).isEqualTo(20);
    }

    @Test
    @DisplayName("limit이 누락되면 400 응답을 반환한다")
    void getContents_missingLimit_returnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/contents")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isBadRequest());

        verify(contentService, never()).findContents(any());
    }

    @Test
    @DisplayName("목록 조회 중 예상하지 못한 예외가 발생하면 500 응답을 반환한다")
    void getContents_unexpectedException_returnsInternalServerError() throws Exception {
        // Given
        given(contentService.findContents(any(ContentCursorRequest.class)))
                .willThrow(new IllegalStateException("unexpected"));

        // When & Then
        mockMvc.perform(get("/api/contents")
                        .param("limit", "20")
                        .param("sortDirection", "DESCENDING")
                        .param("sortBy", "createdAt"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionType").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 에러가 발생했습니다."));
    }

    // --- DELETE ---
    @Test
    @DisplayName("정상적인 콘텐츠 삭제 요청이면 204 응답을 반환한다")
    void deleteContent_success() throws Exception {
        // Given
        UUID contentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        doNothing().when(contentService).delete(contentId);

        // When & Then
        mockMvc.perform(delete("/api/contents/{contentId}", contentId))
                .andExpect(status().isNoContent());

        verify(contentService).delete(contentId);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 삭제 요청이면 404 응답을 반환한다")
    void deleteContent_notFound_returnsNotFound() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        doThrow(new ContentNotFoundException(contentId)).when(contentService).delete(contentId);

        // When & Then
        mockMvc.perform(delete("/api/contents/{contentId}", contentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exceptionType").value("ContentNotFoundException"))
                .andExpect(jsonPath("$.message").value("콘텐츠를 찾을 수 없습니다."));

        verify(contentService).delete(contentId);
    }

    @Test
    @DisplayName("삭제 중 예상하지 못한 예외가 발생하면 500 오류 응답을 반환한다")
    void deleteContent_unexpectedException_returnsInternalServerError() throws Exception {
        // Given
        UUID contentId = UUID.randomUUID();
        doThrow(new IllegalStateException("unexpected")).when(contentService).delete(contentId);

        // When & Then
        mockMvc.perform(delete("/api/contents/{contentId}", contentId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionType").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 에러가 발생했습니다."));
    }
}
