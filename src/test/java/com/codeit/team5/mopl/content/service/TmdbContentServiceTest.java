package com.codeit.team5.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.client.tmdb.TmdbApiClient;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbMovieListResponse;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvDto;
import com.codeit.team5.mopl.content.dto.external.tmdb.TmdbTvListResponse;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import com.codeit.team5.mopl.tag.entity.Tag;
import com.codeit.team5.mopl.tag.repository.TagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class TmdbContentServiceTest {

    @Mock
    private TmdbApiClient tmdbApiClient;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentStatsRepository contentStatsRepository;
    @Mock
    private BinaryContentRepository binaryContentRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TmdbContentService tmdbContentService;

    // TransactionTemplate.execute()가 콜백을 즉시 실행하도록 설정
    @SuppressWarnings("unchecked")
    private void givenTransactionExecutesCallback() {
        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private TmdbMovieDto movieDto(long id, String title, String originalTitle, List<Long> genreIds) {
        return new TmdbMovieDto(id, title, originalTitle, "설명", "/poster.jpg", genreIds, "2023-01-01", 7.5, "en");
    }

    private TmdbTvDto tvDto(long id, String name, String originalName, List<Long> genreIds) {
        return new TmdbTvDto(id, name, originalName, "설명", "/poster.jpg", genreIds, "2023-01-01", 7.5, "en");
    }

    // --- collectMovies ---

    @Test
    @DisplayName("정상적인 영화는 저장된다")
    void collectMovies_savesMovieWithKoreanTitle() {
        givenTransactionExecutesCallback();
        TmdbMovieDto dto = movieDto(1L, "어벤져스", "Avengers", List.of(28L));
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(dto), 1, 1));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "1")).willReturn(false);

        Content saved = mockContent();
        given(contentRepository.save(any())).willReturn(saved);
        given(binaryContentRepository.save(any())).willReturn(mockBinaryContent());
        given(tagRepository.findByNameIn(List.of("액션"))).willReturn(List.of());
        given(tagRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        tmdbContentService.collectMovies(1, 1);

        verify(contentRepository).save(any());
        verify(contentStatsRepository).save(any());
    }

    @Test
    @DisplayName("이미 존재하는 영화는 저장을 건너뛴다")
    void collectMovies_skipsAlreadyExistingMovie() {
        givenTransactionExecutesCallback();
        TmdbMovieDto dto = movieDto(3L, "어벤져스", "Avengers", List.of(28L));
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(dto), 1, 1));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "3")).willReturn(true);

        tmdbContentService.collectMovies(1, 1);

        verify(contentRepository, never()).save(any());
    }

    @Test
    @DisplayName("endPage가 500을 초과하면 500으로 클램핑하여 수집한다")
    void collectMovies_clampsEndPageTo500() {
        givenTransactionExecutesCallback();
        TmdbMovieDto dto = movieDto(4L, "어벤져스", "Avengers", List.of());
        TmdbMovieListResponse response = new TmdbMovieListResponse(1, List.of(dto), 500, 10000);

        // 500번 호출되면 실제로 sleep이 걸리므로 startPage=500, endPage=600으로 테스트
        given(tmdbApiClient.fetchMovies(500)).willReturn(response);
        given(contentRepository.existsBySourceAndExternalId(any(), any())).willReturn(false);
        given(contentRepository.save(any())).willReturn(mockContent());
        given(binaryContentRepository.save(any())).willReturn(mockBinaryContent());
        given(contentStatsRepository.save(any())).willReturn(null);

        tmdbContentService.collectMovies(500, 600);

        verify(tmdbApiClient, times(1)).fetchMovies(500);
        verify(tmdbApiClient, never()).fetchMovies(501);
    }

    @Test
    @DisplayName("여러 페이지를 수집하면 각 페이지마다 API를 호출한다")
    void collectMovies_multiplePages_callsApiForEachPage() {
        givenTransactionExecutesCallback();
        TmdbMovieDto dto = movieDto(5L, "어벤져스", "Avengers", List.of());
        TmdbMovieListResponse response = new TmdbMovieListResponse(1, List.of(dto), 3, 3);

        given(tmdbApiClient.fetchMovies(any(Integer.class))).willReturn(response);
        given(contentRepository.existsBySourceAndExternalId(any(), any())).willReturn(true);

        tmdbContentService.collectMovies(1, 3);

        verify(tmdbApiClient).fetchMovies(1);
        verify(tmdbApiClient).fetchMovies(2);
        verify(tmdbApiClient).fetchMovies(3);
    }

    @Test
    @DisplayName("totalPages가 endPage보다 작으면 totalPages에서 조기 종료한다")
    void collectMovies_stopsAtTotalPages_whenLessThanEndPage() {
        givenTransactionExecutesCallback();
        TmdbMovieDto dto = movieDto(7L, "어벤져스", "Avengers", List.of());
        TmdbMovieListResponse response = new TmdbMovieListResponse(1, List.of(dto), 2, 2);

        given(tmdbApiClient.fetchMovies(any(Integer.class))).willReturn(response);
        given(contentRepository.existsBySourceAndExternalId(any(), any())).willReturn(true);

        tmdbContentService.collectMovies(1, 10);

        verify(tmdbApiClient).fetchMovies(1);
        verify(tmdbApiClient).fetchMovies(2);
        verify(tmdbApiClient, never()).fetchMovies(3);
    }

    @Test
    @DisplayName("posterPath가 없으면 썸네일을 저장하지 않는다")
    void collectMovies_noPosterPath_doesNotSaveThumbnail() {
        givenTransactionExecutesCallback();
        TmdbMovieDto dto = new TmdbMovieDto(6L, "어벤져스", "Avengers", "설명", null, List.of(), "2023-01-01", 7.5, "en");
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(dto), 1, 1));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "6")).willReturn(false);
        given(contentRepository.save(any())).willReturn(mockContent());
        given(contentStatsRepository.save(any())).willReturn(null);

        tmdbContentService.collectMovies(1, 1);

        verify(binaryContentRepository, never()).save(any());
    }

    // --- collectTvSeries ---

    @Test
    @DisplayName("정상적인 TV 시리즈는 저장된다")
    void collectTvSeries_savesTvWithKoreanName() {
        givenTransactionExecutesCallback();
        TmdbTvDto dto = tvDto(10L, "더 글로리", "The Glory", List.of(18L));
        given(tmdbApiClient.fetchTvSeries(1)).willReturn(new TmdbTvListResponse(1, List.of(dto), 1, 1));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "10")).willReturn(false);
        given(contentRepository.save(any())).willReturn(mockContent());
        given(binaryContentRepository.save(any())).willReturn(mockBinaryContent());
        given(tagRepository.findByNameIn(List.of("드라마"))).willReturn(List.of());
        given(tagRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        tmdbContentService.collectTvSeries(1, 1);

        verify(contentRepository).save(any());
        verify(contentStatsRepository).save(any());
    }

    @Test
    @DisplayName("이미 존재하는 TV 시리즈는 저장을 건너뛴다")
    void collectTvSeries_skipsAlreadyExisting() {
        givenTransactionExecutesCallback();
        TmdbTvDto dto = tvDto(12L, "더 글로리", "The Glory", List.of());
        given(tmdbApiClient.fetchTvSeries(1)).willReturn(new TmdbTvListResponse(1, List.of(dto), 1, 1));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "12")).willReturn(true);

        tmdbContentService.collectTvSeries(1, 1);

        verify(contentRepository, never()).save(any());
    }

    @Test
    @DisplayName("장르 ID가 매핑되면 태그로 저장되고, 알 수 없는 장르 ID는 무시된다")
    void collectTvSeries_attachesKnownGenreTags() {
        givenTransactionExecutesCallback();
        TmdbTvDto dto = tvDto(13L, "더 글로리", "The Glory", List.of(18L, 99999L));
        given(tmdbApiClient.fetchTvSeries(1)).willReturn(new TmdbTvListResponse(1, List.of(dto), 1, 1));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "13")).willReturn(false);

        Content saved = mockContent();
        given(contentRepository.save(any())).willReturn(saved);
        given(binaryContentRepository.save(any())).willReturn(mockBinaryContent());
        given(tagRepository.findByNameIn(List.of("드라마"))).willReturn(List.of());  // 원본 라벨로 조회
        ArgumentCaptor<List> tagListCaptor = ArgumentCaptor.forClass(List.class);
        given(tagRepository.saveAll(tagListCaptor.capture())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        tmdbContentService.collectTvSeries(1, 1);

        List<Tag> savedTags = tagListCaptor.getValue();
        assertThat(savedTags).hasSize(1);
        assertThat(((Tag) savedTags.get(0)).getName()).isEqualTo("드라마");
    }

    @Test
    @DisplayName("이미 존재하는 태그는 새로 저장하지 않는다")
    void collectTvSeries_reusesExistingTag() {
        givenTransactionExecutesCallback();
        TmdbTvDto dto = tvDto(14L, "더 글로리", "The Glory", List.of(18L));
        given(tmdbApiClient.fetchTvSeries(1)).willReturn(new TmdbTvListResponse(1, List.of(dto), 1, 1));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "14")).willReturn(false);

        Content saved = mockContent();
        given(contentRepository.save(any())).willReturn(saved);
        given(binaryContentRepository.save(any())).willReturn(mockBinaryContent());

        Tag existingTag = Tag.create("드라마");
        given(tagRepository.findByNameIn(List.of("드라마"))).willReturn(List.of(existingTag));
        given(contentStatsRepository.save(any())).willReturn(null);

        tmdbContentService.collectTvSeries(1, 1);

        verify(tagRepository, never()).save(any());
    }

    @Test
    @DisplayName("날짜 형식이 잘못된 경우 null로 저장된다")
    void collectMovies_invalidDate_savesWithNullDate() {
        givenTransactionExecutesCallback();
        TmdbMovieDto dto = new TmdbMovieDto(20L, "어벤져스", "Avengers", "설명", null, List.of(), "invalid-date", 7.5, "en");
        given(tmdbApiClient.fetchMovies(1)).willReturn(new TmdbMovieListResponse(1, List.of(dto), 1, 1));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.TMDB, "20")).willReturn(false);

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        given(contentRepository.save(contentCaptor.capture())).willReturn(mockContent());
        given(contentStatsRepository.save(any())).willReturn(null);

        tmdbContentService.collectMovies(1, 1);

        assertThat(contentCaptor.getValue().getReleasedAt()).isNull();
    }

    private Content mockContent() {
        return Content.createByExternalSource(
                ContentType.MOVIE, "어벤져스", "설명",
                ContentSource.TMDB, "1", null, "{}"
        );
    }

    private BinaryContent mockBinaryContent() {
        return BinaryContent.externalUrl("https://image.tmdb.org/t/p/w500/poster.jpg");
    }
}
