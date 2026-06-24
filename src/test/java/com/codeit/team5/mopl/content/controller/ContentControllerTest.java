package com.codeit.team5.mopl.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.service.ContentService;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(ContentController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContentService contentService;

    @Captor
    private ArgumentCaptor<ContentCreateRequest> requestCaptor;

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
                null,
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
                .andExpect(jsonPath("$.tags[0]").value("액션"))
                .andExpect(jsonPath("$.tags[1]").value("드라마"))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.watcherCount").value(0));

        verify(contentService).create(requestCaptor.capture(), any(MultipartFile.class));
        ContentCreateRequest captured = requestCaptor.getValue();
        assertThat(captured.type()).isEqualTo(ContentType.MOVIE);
        assertThat(captured.title()).isEqualTo("테스트 영화");
        assertThat(captured.description()).isEqualTo("테스트 설명");
        assertThat(captured.tags()).containsExactly("액션", "드라마");
    }

    //todo 현재는 썸네일 처리 로직이 없기에 썸네일이 있는 테스트 코드와 다르지 않음
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
                .andExpect(jsonPath("$.exceptionName").value("INVALID_INPUT"))
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
                .andExpect(jsonPath("$.exceptionName").value("INVALID_INPUT"))
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
                .andExpect(jsonPath("$.exceptionName").value("INVALID_INPUT"))
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
                .andExpect(jsonPath("$.exceptionName").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }
}
