package com.codeit.team5.mopl.content.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentStatsCacheStoreTest {

    @Mock
    private ContentStatsRepository contentStatsRepository;

    @InjectMocks
    private ContentStatsCacheStore contentStatsCacheStore;

    @Test
    @DisplayName("ContentStats가 존재하면 reviewCount와 averageRating을 반환한다")
    void getRatingStats_statsFound_returnsStats() {
        // given
        UUID contentId = UUID.randomUUID();
        ContentStats stats = org.mockito.Mockito.mock(ContentStats.class);
        given(stats.getReviewCount()).willReturn(5);
        given(stats.getAverageRating()).willReturn(4.2);
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.of(stats));

        // when
        ContentRatingStats result = contentStatsCacheStore.getRatingStats(contentId);

        // then
        assertThat(result.reviewCount()).isEqualTo(5);
        assertThat(result.averageRating()).isEqualTo(4.2);
    }

    @Test
    @DisplayName("ContentStats가 없으면 reviewCount=0, averageRating=0.0을 반환한다")
    void getRatingStats_statsNotFound_returnsEmpty() {
        // given
        UUID contentId = UUID.randomUUID();
        given(contentStatsRepository.findById(contentId)).willReturn(Optional.empty());

        // when
        ContentRatingStats result = contentStatsCacheStore.getRatingStats(contentId);

        // then
        assertThat(result.reviewCount()).isZero();
        assertThat(result.averageRating()).isZero();
    }
}
