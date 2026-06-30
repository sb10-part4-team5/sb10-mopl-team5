package com.codeit.team5.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.client.sportsdb.SportsDbApiClient;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventDto;
import com.codeit.team5.mopl.content.dto.external.sportsdb.SportsDbEventListResponse;
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
class SportsDbContentServiceTest {

    @Mock
    private SportsDbApiClient sportsDbApiClient;
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
    private SportsDbContentService sportsDbContentService;

    @SuppressWarnings("unchecked")
    private void givenTransactionExecutesCallback() {
        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private SportsDbEventDto eventDto(String idEvent, String strEvent, String strLeague,
                                       String thumbUrl, String date,
                                       String homeScore, String awayScore) {
        return new SportsDbEventDto(idEvent, strEvent, thumbUrl, date,
                strLeague, "Soccer", "Home FC", "Away FC", homeScore, awayScore);
    }

    // --- collectEvents ---

    @Test
    @DisplayName("정상적인 경기 이벤트는 저장된다")
    void collectEvents_savesEvent() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto = eventDto("1001", "Home FC vs Away FC", "English Premier League",
                "https://thumb.url/img.jpg", "2023-08-12", "2", "1");
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "1001")).willReturn(false);

        Content saved = mockContent();
        given(contentRepository.save(any())).willReturn(saved);
        given(binaryContentRepository.save(any())).willReturn(mockBinaryContent());
        given(tagRepository.findByName("english premier league")).willReturn(Optional.empty());
        given(tagRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        verify(contentRepository).save(any());
        verify(contentStatsRepository).save(any());
    }

    @Test
    @DisplayName("API 응답 자체가 null이면 저장을 건너뛴다")
    void collectEvents_nullResponse_skipsAll() {
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024")).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        verify(contentRepository, never()).save(any());
    }

    @Test
    @DisplayName("API 응답의 events가 null이면 저장을 건너뛴다")
    void collectEvents_nullEvents_skipsAll() {
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(null));

        sportsDbContentService.collectEvents("4328", "2023-2024");

        verify(contentRepository, never()).save(any());
    }

    @Test
    @DisplayName("API 응답의 events가 빈 배열이면 저장을 건너뛴다")
    void collectEvents_emptyEvents_skipsAll() {
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of()));

        sportsDbContentService.collectEvents("4328", "2023-2024");

        verify(contentRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 존재하는 경기는 저장을 건너뛴다")
    void collectEvents_skipsAlreadyExistingEvent() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto = eventDto("2001", "Match", "EPL", null, "2023-09-01", null, null);
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "2001")).willReturn(true);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        verify(contentRepository, never()).save(any());
    }

    @Test
    @DisplayName("썸네일 URL이 없으면 BinaryContent를 저장하지 않는다")
    void collectEvents_noThumbUrl_doesNotSaveThumbnail() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto = eventDto("3001", "Match", "English Premier League", null, "2023-09-01", null, null);
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "3001")).willReturn(false);
        given(contentRepository.save(any())).willReturn(mockContent());
        given(tagRepository.findByName(any())).willReturn(Optional.empty());
        given(tagRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        verify(binaryContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("리그명이 소문자로 정규화되어 태그로 저장된다")
    void collectEvents_normalizedLeagueTagSaved() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto = eventDto("4001", "Match", "English Premier League",
                null, "2023-09-01", null, null);
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "4001")).willReturn(false);
        given(contentRepository.save(any())).willReturn(mockContent());
        given(tagRepository.findByName("english premier league")).willReturn(Optional.empty());

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        given(tagRepository.save(tagCaptor.capture())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        assertThat(tagCaptor.getValue().getName()).isEqualTo("english premier league");
    }

    @Test
    @DisplayName("이미 존재하는 리그 태그는 새로 저장하지 않는다")
    void collectEvents_reusesExistingLeagueTag() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto = eventDto("5001", "Match", "English Premier League",
                null, "2023-09-01", null, null);
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "5001")).willReturn(false);
        given(contentRepository.save(any())).willReturn(mockContent());

        Tag existingTag = Tag.create("english premier league");
        given(tagRepository.findByName("english premier league")).willReturn(Optional.of(existingTag));
        given(contentStatsRepository.save(any())).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        verify(tagRepository, never()).save(any());
    }

    @Test
    @DisplayName("스코어가 있으면 설명에 점수가 포함된다")
    void collectEvents_withScore_includesScoreInDescription() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto = eventDto("6001", "Home FC vs Away FC", "EPL",
                null, "2023-09-01", "3", "0");
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "6001")).willReturn(false);

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        given(contentRepository.save(contentCaptor.capture())).willReturn(mockContent());
        given(tagRepository.findByName(any())).willReturn(Optional.empty());
        given(tagRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        assertThat(contentCaptor.getValue().getDescription()).contains("3 - 0");
    }

    @Test
    @DisplayName("스코어가 없으면 설명에 vs가 포함된다")
    void collectEvents_withoutScore_includesVsInDescription() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto = eventDto("7001", "Home FC vs Away FC", "EPL",
                null, "2023-09-01", null, null);
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "7001")).willReturn(false);

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        given(contentRepository.save(contentCaptor.capture())).willReturn(mockContent());
        given(tagRepository.findByName(any())).willReturn(Optional.empty());
        given(tagRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        assertThat(contentCaptor.getValue().getDescription()).contains("vs");
    }

    @Test
    @DisplayName("날짜 형식이 잘못된 경우 null로 저장된다")
    void collectEvents_invalidDate_savesWithNullDate() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto = eventDto("8001", "Match", "EPL", null, "not-a-date", null, null);
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "8001")).willReturn(false);

        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        given(contentRepository.save(contentCaptor.capture())).willReturn(mockContent());
        given(tagRepository.findByName(any())).willReturn(Optional.empty());
        given(tagRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        assertThat(contentCaptor.getValue().getReleasedAt()).isNull();
    }

    @Test
    @DisplayName("여러 경기가 있으면 각각 저장 여부를 확인하고 처리한다")
    void collectEvents_multipleEvents_processesAll() {
        givenTransactionExecutesCallback();
        SportsDbEventDto dto1 = eventDto("9001", "Match1", "EPL", null, "2023-09-01", null, null);
        SportsDbEventDto dto2 = eventDto("9002", "Match2", "EPL", null, "2023-09-08", null, null);
        given(sportsDbApiClient.fetchEventsBySeason("4328", "2023-2024"))
                .willReturn(new SportsDbEventListResponse(List.of(dto1, dto2)));
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "9001")).willReturn(false);
        given(contentRepository.existsBySourceAndExternalId(ContentSource.SPORTS_DB, "9002")).willReturn(true);
        given(contentRepository.save(any())).willReturn(mockContent());
        given(tagRepository.findByName(any())).willReturn(Optional.empty());
        given(tagRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(contentStatsRepository.save(any())).willReturn(null);

        sportsDbContentService.collectEvents("4328", "2023-2024");

        verify(contentRepository, org.mockito.Mockito.times(1)).save(any());
    }

    private Content mockContent() {
        return Content.createByExternalSource(
                ContentType.SPORT, "Home FC vs Away FC", "EPL Home FC 2 - 1 Away FC",
                ContentSource.SPORTS_DB, "1001", null, "{}"
        );
    }

    private BinaryContent mockBinaryContent() {
        return BinaryContent.externalUrl("https://thumb.url/img.jpg");
    }
}
