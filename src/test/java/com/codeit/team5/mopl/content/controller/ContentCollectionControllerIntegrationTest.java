package com.codeit.team5.mopl.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ContentCollectionController 통합 테스트.
 *
 * <p>실제 시큐리티 필터체인, 요청 검증·타입 변환(컨버터), 컨트롤러 로직을 모두 주입하여 수집
 * 엔드포인트의 인증/인가(401/403), 요청 검증(400), Job 실행 경로(202/500)를 한곳에서 검증한다.</p>
 *
 * <p>실제 배치 Job은 외부 API(TMDB/SportsDB)를 호출하므로, JobLauncher와 Job 빈만 목으로 대체하여
 * "요청이 정상적으로 Job 실행 경로까지 도달하는지(202)"와 "실행 실패 시 500"만 검증한다.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ContentCollectionControllerIntegrationTest {

    private static final String MOVIES_URL = "/api/admin/contents/collect/tmdb/movies";
    private static final String TV_URL = "/api/admin/contents/collect/tmdb/tv";
    private static final String SPORTS_URL = "/api/admin/contents/collect/sports";
    private static final String SPORTS_DAY_URL = "/api/admin/contents/collect/sports/day";

    @Autowired
    private MockMvc mockMvc;

    // 실제 Job 빈은 그대로 두고(배치 JobRegistry가 getName()으로 등록하므로 목으로 대체하면 NPE 발생),
    // JobLauncher만 목으로 대체해 실제 배치 실행(외부 API 호출)을 차단한다.
    @MockitoBean(name = "asyncJobLauncher")
    private JobLauncher asyncJobLauncher;

    // --- 인증 헬퍼 ---

    private Authentication adminAuth() {
        return authOf("admin@mopl.com", "ADMIN");
    }

    private Authentication userAuth() {
        return authOf("user@mopl.com", "USER");
    }

    private Authentication authOf(String email, String role) {
        MoplUserDetails details = new MoplUserDetails(
                new AuthUser(UUID.randomUUID(), email, role, false), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    @Nested
    @DisplayName("POST /tmdb/movies - TMDB 영화 수집")
    class CollectTmdbMovies {

        @Test
        @DisplayName("ADMIN이 유효한 페이지 범위로 요청하면 202를 반환한다")
        void success_returnsAccepted() throws Exception {
            // when & then
            mockMvc.perform(post(MOVIES_URL)
                            .param("startPage", "1")
                            .param("endPage", "5")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("USER 권한이면 403을 반환한다")
        void asUser_returnsForbidden() throws Exception {
            // when & then
            mockMvc.perform(post(MOVIES_URL)
                            .param("startPage", "1")
                            .param("endPage", "5")
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void unauthenticated_returnsUnauthorized() throws Exception {
            // when & then
            mockMvc.perform(post(MOVIES_URL)
                            .param("startPage", "1")
                            .param("endPage", "5")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("페이지 파라미터가 누락되면 400을 반환한다")
        void missingParams_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(MOVIES_URL)
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("startPage가 0 이하면 400을 반환한다")
        void invalidStartPage_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(MOVIES_URL)
                            .param("startPage", "0")
                            .param("endPage", "5")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("endPage가 startPage보다 작으면 400을 반환한다")
        void endPageLessThanStartPage_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(MOVIES_URL)
                            .param("startPage", "5")
                            .param("endPage", "3")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Job 실행 자체가 실패하면 500을 반환한다")
        void jobLaunchFails_returnsInternalServerError() throws Exception {
            // given
            given(asyncJobLauncher.run(any(), any()))
                    .willThrow(new JobParametersInvalidException("잘못된 Job 파라미터"));

            // when & then
            mockMvc.perform(post(MOVIES_URL)
                            .param("startPage", "1")
                            .param("endPage", "5")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /tmdb/tv - TMDB TV 시리즈 수집")
    class CollectTmdbTvSeries {

        @Test
        @DisplayName("ADMIN이 유효한 페이지 범위로 요청하면 202를 반환한다")
        void success_returnsAccepted() throws Exception {
            // when & then
            mockMvc.perform(post(TV_URL)
                            .param("startPage", "2")
                            .param("endPage", "10")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("USER 권한이면 403을 반환한다")
        void asUser_returnsForbidden() throws Exception {
            // when & then
            mockMvc.perform(post(TV_URL)
                            .param("startPage", "2")
                            .param("endPage", "10")
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void unauthenticated_returnsUnauthorized() throws Exception {
            // when & then
            mockMvc.perform(post(TV_URL)
                            .param("startPage", "2")
                            .param("endPage", "10")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("페이지 파라미터가 누락되면 400을 반환한다")
        void missingParams_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(TV_URL)
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("endPage가 startPage보다 작으면 400을 반환한다")
        void endPageLessThanStartPage_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(TV_URL)
                            .param("startPage", "10")
                            .param("endPage", "5")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /sports - SportsDB 경기 수집")
    class CollectSportsEvents {

        @Test
        @DisplayName("ADMIN이 유효한 리그·시즌으로 요청하면 202를 반환한다")
        void success_returnsAccepted() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_URL)
                            .param("league", "EPL")
                            .param("season", "2023-2024")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("USER 권한이면 403을 반환한다")
        void asUser_returnsForbidden() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_URL)
                            .param("league", "EPL")
                            .param("season", "2023-2024")
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void unauthenticated_returnsUnauthorized() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_URL)
                            .param("league", "EPL")
                            .param("season", "2023-2024")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("league 파라미터가 누락되면 400을 반환한다")
        void missingLeague_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_URL)
                            .param("season", "2023-2024")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효하지 않은 리그 값이면 400을 반환한다")
        void invalidLeague_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_URL)
                            .param("league", "INVALID_LEAGUE")
                            .param("season", "2023-2024")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("시즌 형식이 YYYY-YYYY가 아니면 400을 반환한다")
        void invalidSeasonFormat_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_URL)
                            .param("league", "EPL")
                            .param("season", "2023/2024")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /sports/day - SportsDB 일별 경기 수집")
    class CollectSportsEventsByDay {

        @Test
        @DisplayName("ADMIN이 유효한 날짜로 요청하면 202를 반환한다")
        void success_returnsAccepted() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_DAY_URL)
                            .param("date", "2024-12-26")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isAccepted());
        }

        @Test
        @DisplayName("USER 권한이면 403을 반환한다")
        void asUser_returnsForbidden() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_DAY_URL)
                            .param("date", "2024-12-26")
                            .with(csrf())
                            .with(authentication(userAuth())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void unauthenticated_returnsUnauthorized() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_DAY_URL)
                            .param("date", "2024-12-26")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("date 파라미터가 누락되면 400을 반환한다")
        void missingDate_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_DAY_URL)
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("날짜 형식이 YYYY-MM-DD가 아니면 400을 반환한다")
        void invalidDateFormat_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_DAY_URL)
                            .param("date", "2024/12/26")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실존하지 않는 날짜면 400을 반환한다")
        void nonExistentDate_returnsBadRequest() throws Exception {
            // when & then
            mockMvc.perform(post(SPORTS_DAY_URL)
                            .param("date", "2024-02-31")
                            .with(csrf())
                            .with(authentication(adminAuth())))
                    .andExpect(status().isBadRequest());
        }
    }
}
