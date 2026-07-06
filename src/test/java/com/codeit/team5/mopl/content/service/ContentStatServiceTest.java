package com.codeit.team5.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentStats;
import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentStatServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private ContentStatService contentStatService;

    @Test
    @DisplayName("리뷰 생성 시 ratingSum과 reviewCount가 증가한다")
    void updateContentStat_create() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = mock(Content.class);
        ContentStats stats = mock(ContentStats.class);
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(content.getStats()).willReturn(stats);
        given(stats.getReviewCount()).willReturn(2);
        given(stats.getRatingSum()).willReturn(8.0);

        // when
        contentStatService.updateContentStat(contentId, 4.0, 1);

        // then
        verify(stats).updateRating(12.0, 3);
    }

    @Test
    @DisplayName("리뷰 수정 시 ratingDelta만큼 ratingSum이 변경되고 reviewCount는 유지된다")
    void updateContentStat_update() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = mock(Content.class);
        ContentStats stats = mock(ContentStats.class);
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(content.getStats()).willReturn(stats);
        given(stats.getReviewCount()).willReturn(2);
        given(stats.getRatingSum()).willReturn(8.0);

        // when
        contentStatService.updateContentStat(contentId, 1.5, 0);

        // then
        verify(stats).updateRating(9.5, 2);
    }

    @Test
    @DisplayName("리뷰 삭제 후 리뷰가 남아있으면 ratingSum에서 해당 rating이 차감된다")
    void updateContentStat_delete_remainingReviews() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = mock(Content.class);
        ContentStats stats = mock(ContentStats.class);
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(content.getStats()).willReturn(stats);
        given(stats.getReviewCount()).willReturn(2);
        given(stats.getRatingSum()).willReturn(8.0);

        // when
        contentStatService.updateContentStat(contentId, -4.0, -1);

        // then
        verify(stats).updateRating(4.0, 1);
    }

    @Test
    @DisplayName("마지막 리뷰 삭제 시 reviewCount가 0이 되면 ratingSum도 0으로 초기화된다")
    void updateContentStat_delete_lastReview() {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = mock(Content.class);
        ContentStats stats = mock(ContentStats.class);
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(content.getStats()).willReturn(stats);
        given(stats.getReviewCount()).willReturn(1);
        given(stats.getRatingSum()).willReturn(4.5);

        // when
        contentStatService.updateContentStat(contentId, -4.5, -1);

        // then
        verify(stats).updateRating(0.0, 0);
    }

    @Test
    @DisplayName("콘텐츠가 존재하지 않으면 예외가 발생한다")
    void updateContentStat_contentNotFound() {
        // given
        UUID contentId = UUID.randomUUID();
        given(contentRepository.findById(contentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contentStatService.updateContentStat(contentId, 4.5, 1))
            .isInstanceOf(ContentNotFoundException.class);
    }
}
