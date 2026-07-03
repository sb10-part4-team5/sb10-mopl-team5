package com.codeit.team5.mopl.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContentUploadStatus;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.dto.request.ContentCreateRequest;
import com.codeit.team5.mopl.content.dto.request.ContentUpdateRequest;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentTag;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * ContentController 통합 테스트.
 *
 * <p>{@code @WebMvcTest} 기반 단위 테스트({@link ContentControllerTest})와 달리 실제 시큐리티 필터체인,
 * ContentService, Repository, ContentMapper, 검증 로직을 모두 주입하여 Testcontainers DB에 실제로
 * 반영되는지 검증한다.</p>
 *
 * <p>썸네일 업로드(S3) 경로는 이 컨트롤러 통합 테스트에서 다루지 않는다.
 * ({@code UserControllerIntegrationTest}와 동일한 컨벤션 — 이미지 업로드/스토리지 검증은 서비스 레벨
 * 통합 테스트의 책임으로 분리한다.)</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ContentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentStatsRepository contentStatsRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private BinaryContentRepository binaryContentRepository;

    // --- 인증 헬퍼 ---

    private Authentication adminAuth() {
        return authOf(UUID.randomUUID(), "admin1234@admin.com", "ADMIN");
    }

    private Authentication userAuth() {
        return authOf(UUID.randomUUID(), "user@mopl.com", "USER");
    }

    private Authentication authOf(UUID userId, String email, String role) {
        MoplUserDetails details = new MoplUserDetails(
                new AuthUser(userId, email, role, false), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    // --- 데이터 준비 헬퍼 ---

    private Content persistContent(ContentType type, String title, String description, String... tagNames) {
        Content content = contentRepository.save(Content.createByAdmin(type, title, description));
        ContentStats stats = contentStatsRepository.save(ContentStats.create(content));
        content.attachStats(stats);
        for (String name : tagNames) {
            // 여러 콘텐츠가 같은 태그를 공유할 수 있으므로 기존 태그를 재사용한다 (tags.name 유니크 제약)
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(Tag.create(name)));
            content.addTag(ContentTag.create(content, tag));
        }
        return contentRepository.saveAndFlush(content);
    }

    private Set<String> tagNamesOf(Content content) {
        return content.getContentTags().stream()
                .map(ct -> ct.getTag().getName())
                .collect(Collectors.toSet());
    }

    private MockMultipartFile requestPart(Object request) throws Exception {
        return new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
    }

    @Nested
    @DisplayName("POST /api/contents - 콘텐츠 생성")
    class PostContent {

        @Test
        @DisplayName("ADMIN이 생성하면 201과 함께 DB에 콘텐츠·통계·태그가 저장되고 태그가 정규화된다")
        void create_persists() throws Exception {
            // given
            ContentCreateRequest request = new ContentCreateRequest(
                    ContentType.MOVIE, "테스트 영화", "설명", List.of("액션", " 액션 ", "드라마"));

            // when & then
            String body = mockMvc.perform(multipart("/api/contents")
                            .file(requestPart(request))
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.type").value("movie"))
                    .andExpect(jsonPath("$.title").value("테스트 영화"))
                    .andExpect(jsonPath("$.description").value("설명"))
                    .andExpect(jsonPath("$.thumbnailUrl").doesNotExist())
                    .andExpect(jsonPath("$.averageRating").value(0.0))
                    .andExpect(jsonPath("$.reviewCount").value(0))
                    .andExpect(jsonPath("$.watcherCount").value(0))
                    .andReturn().getResponse().getContentAsString();

            UUID contentId = UUID.fromString(objectMapper.readTree(body).get("id").asText());
            Content saved = contentRepository.findWithStatsAndTagsById(contentId).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("테스트 영화");
            assertThat(saved.getType()).isEqualTo(ContentType.MOVIE);
            assertThat(saved.getStats()).isNotNull();
            // 태그는 trim·중복 제거되어 2개만 저장되어야 한다
            assertThat(tagNamesOf(saved)).containsExactlyInAnyOrder("액션", "드라마");
        }

        @Test
        @DisplayName("USER 권한이면 403과 함께 아무것도 저장되지 않는다")
        void create_asUser_forbidden() throws Exception {
            // given
            ContentCreateRequest request = new ContentCreateRequest(
                    ContentType.MOVIE, "테스트 영화", null, List.of("액션"));

            // when & then
            mockMvc.perform(multipart("/api/contents")
                            .file(requestPart(request))
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());

            assertThat(contentRepository.count()).isZero();
        }

        @Test
        @DisplayName("인증 없이 생성하면 401을 반환한다")
        void create_unauthenticated_unauthorized() throws Exception {
            // given
            ContentCreateRequest request = new ContentCreateRequest(
                    ContentType.MOVIE, "테스트 영화", null, List.of("액션"));

            // when & then
            mockMvc.perform(multipart("/api/contents")
                            .file(requestPart(request))
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());

            assertThat(contentRepository.count()).isZero();
        }

        @Test
        @DisplayName("제목이 공백이면 400 검증 실패를 반환한다")
        void create_blankTitle_badRequest() throws Exception {
            // given
            ContentCreateRequest request = new ContentCreateRequest(
                    ContentType.MOVIE, "   ", null, List.of("액션"));

            // when & then
            mockMvc.perform(multipart("/api/contents")
                            .file(requestPart(request))
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"));

            assertThat(contentRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("PATCH /api/contents/{id} - 콘텐츠 수정")
    class PatchContent {

        @Test
        @DisplayName("ADMIN이 수정하면 200과 함께 제목·설명·태그가 DB에 반영된다")
        void update_persists() throws Exception {
            // given
            Content content = persistContent(ContentType.MOVIE, "원본 영화", "원본 설명", "액션", "드라마");
            ContentUpdateRequest request = new ContentUpdateRequest(
                    "수정된 영화", "수정된 설명", List.of("액션", "sf"));

            // when & then
            mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{id}", content.getId())
                            .file(requestPart(request))
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("수정된 영화"))
                    .andExpect(jsonPath("$.description").value("수정된 설명"));

            Content updated = contentRepository.findWithStatsAndTagsById(content.getId()).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("수정된 영화");
            assertThat(updated.getDescription()).isEqualTo("수정된 설명");
            // "드라마"는 제거되고 "sf"가 추가되어야 한다 (diff 반영)
            assertThat(tagNamesOf(updated)).containsExactlyInAnyOrder("액션", "sf");
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 수정이면 404를 반환한다")
        void update_notFound() throws Exception {
            // given
            ContentUpdateRequest request = new ContentUpdateRequest("수정", null, List.of("액션"));

            // when & then
            mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{id}", UUID.randomUUID())
                            .file(requestPart(request))
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.exceptionType").value("ContentNotFoundException"));
        }

        @Test
        @DisplayName("USER 권한이면 403과 함께 수정되지 않는다")
        void update_asUser_forbidden() throws Exception {
            // given
            Content content = persistContent(ContentType.MOVIE, "원본 영화", null, "액션");
            ContentUpdateRequest request = new ContentUpdateRequest("수정된 영화", null, List.of("sf"));

            // when & then
            mockMvc.perform(multipart(HttpMethod.PATCH, "/api/contents/{id}", content.getId())
                            .file(requestPart(request))
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());

            Content unchanged = contentRepository.findWithStatsAndTagsById(content.getId()).orElseThrow();
            assertThat(unchanged.getTitle()).isEqualTo("원본 영화");
        }
    }

    @Nested
    @DisplayName("GET /api/contents/{id} - 단건 조회")
    class GetContent {

        @Test
        @DisplayName("존재하는 콘텐츠를 인증 사용자가 조회하면 200과 함께 통계·태그를 반환한다")
        void getById_success() throws Exception {
            // given
            Content content = persistContent(ContentType.MOVIE, "조회 영화", "설명", "액션", "sf");

            // when & then
            mockMvc.perform(get("/api/contents/{id}", content.getId())
                            .with(authentication(userAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(content.getId().toString()))
                    .andExpect(jsonPath("$.title").value("조회 영화"))
                    .andExpect(jsonPath("$.tags", org.hamcrest.Matchers.containsInAnyOrder("액션", "sf")))
                    .andExpect(jsonPath("$.averageRating").value(0.0));
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 조회면 404를 반환한다")
        void getById_notFound() throws Exception {
            // when & then
            mockMvc.perform(get("/api/contents/{id}", UUID.randomUUID())
                            .with(authentication(userAuth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.exceptionType").value("ContentNotFoundException"));
        }

        @Test
        @DisplayName("인증 없이 단건 조회면 401을 반환한다")
        void getById_unauthenticated() throws Exception {
            // given
            Content content = persistContent(ContentType.MOVIE, "조회 영화", null, "액션");

            // when & then
            mockMvc.perform(get("/api/contents/{id}", content.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/contents - 목록 조회")
    class GetContents {

        @Test
        @DisplayName("타입·키워드 필터로 조회하면 조건에 맞는 콘텐츠만 반환한다")
        void list_withFilters() throws Exception {
            // given
            persistContent(ContentType.MOVIE, "어벤져스", "히어로", "액션");
            persistContent(ContentType.MOVIE, "인터스텔라", "우주", "sf");
            persistContent(ContentType.TV_SERIES, "어벤져스 드라마", null, "액션");

            // when & then
            mockMvc.perform(get("/api/contents")
                            .param("typeEqual", "movie")
                            .param("keywordLike", "어벤져스")
                            .param("limit", "20")
                            .param("sortDirection", "DESCENDING")
                            .param("sortBy", "createdAt")
                            .with(authentication(userAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("어벤져스"))
                    .andExpect(jsonPath("$.totalCount").value(1))
                    .andExpect(jsonPath("$.hasNext").value(false))
                    .andExpect(jsonPath("$.sortBy").value("createdAt"))
                    .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
        }

        @Test
        @DisplayName("limit보다 데이터가 많으면 hasNext=true와 nextCursor를 반환한다")
        void list_pagination_hasNext() throws Exception {
            // given
            for (int i = 0; i < 3; i++) {
                persistContent(ContentType.MOVIE, "영화" + i, null, "액션");
            }

            // when & then
            mockMvc.perform(get("/api/contents")
                            .param("limit", "2")
                            .param("sortDirection", "DESCENDING")
                            .param("sortBy", "createdAt")
                            .with(authentication(userAuth())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.hasNext").value(true))
                    .andExpect(jsonPath("$.nextCursor").exists())
                    .andExpect(jsonPath("$.totalCount").value(3));
        }

        @Test
        @DisplayName("limit이 누락되면 400을 반환한다")
        void list_missingLimit_badRequest() throws Exception {
            // when & then
            mockMvc.perform(get("/api/contents")
                            .param("sortDirection", "DESCENDING")
                            .param("sortBy", "createdAt")
                            .with(authentication(userAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증 없이 목록 조회면 401을 반환한다")
        void list_unauthenticated() throws Exception {
            // when & then
            mockMvc.perform(get("/api/contents")
                            .param("limit", "20")
                            .param("sortDirection", "DESCENDING")
                            .param("sortBy", "createdAt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/contents/{id} - 콘텐츠 삭제")
    class DeleteContent {

        @Test
        @DisplayName("ADMIN이 삭제하면 204와 함께 DB에서 제거되고 썸네일은 DELETED로 마킹된다")
        void delete_persists() throws Exception {
            // given
            Content content = persistContent(ContentType.MOVIE, "삭제 영화", null, "액션");
            // 스토리지 업로드 없이 이미 저장된 썸네일을 붙여 삭제 시 상태 전이만 검증한다
            BinaryContent thumbnail = binaryContentRepository.save(
                    BinaryContent.completed("http://localhost/thumbnails/old.jpg"));
            content.attachThumbnail(thumbnail);
            contentRepository.saveAndFlush(content);
            UUID thumbnailId = thumbnail.getId();

            // when & then
            mockMvc.perform(delete("/api/contents/{id}", content.getId())
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isNoContent());

            assertThat(contentRepository.findById(content.getId())).isEmpty();
            BinaryContent reloaded = binaryContentRepository.findById(thumbnailId).orElseThrow();
            assertThat(reloaded.getUploadStatus()).isEqualTo(BinaryContentUploadStatus.DELETED);
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 삭제면 404를 반환한다")
        void delete_notFound() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/contents/{id}", UUID.randomUUID())
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.exceptionType").value("ContentNotFoundException"));
        }

        @Test
        @DisplayName("USER 권한이면 403과 함께 삭제되지 않는다")
        void delete_asUser_forbidden() throws Exception {
            // given
            Content content = persistContent(ContentType.MOVIE, "삭제 영화", null, "액션");

            // when & then
            mockMvc.perform(delete("/api/contents/{id}", content.getId())
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());

            assertThat(contentRepository.findById(content.getId())).isPresent();
        }
    }
}
