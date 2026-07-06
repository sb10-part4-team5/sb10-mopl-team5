package com.codeit.team5.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.content.exception.ContentNotFoundException;
import com.codeit.team5.mopl.content.repository.ContentStatsRepository;
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
    private ContentStatsRepository contentStatsRepository;

    @InjectMocks
    private ContentStatService contentStatService;

    @Test
    @DisplayName("리뷰 생성 시 ratingDelta와 countDelta가 +로 전달된다")
    void reviewUpdateContentStat_create() {
        // given
        UUID contentId = UUID.randomUUID();
        given(contentStatsRepository.applyStatDelta(contentId, 4.0, 1)).willReturn(1);

        // when
        contentStatService.reviewUpdateContentStat(contentId, 4.0, 1);

        // then
        verify(contentStatsRepository).applyStatDelta(contentId, 4.0, 1);
    }

    @Test
    @DisplayName("리뷰 수정 시 countDelta=0으로 ratingDelta만 전달된다")
    void updateContentStat_reviewUpdate() {
        // given
        UUID contentId = UUID.randomUUID();
        given(contentStatsRepository.applyStatDelta(contentId, 1.5, 0)).willReturn(1);

        // when
        contentStatService.reviewUpdateContentStat(contentId, 1.5, 0);

        // then
        verify(contentStatsRepository).applyStatDelta(contentId, 1.5, 0);
    }

    @Test
    @DisplayName("리뷰 삭제 시 ratingDelta와 countDelta가 -로 전달된다")
    void reviewUpdateContentStat_delete() {
        // given
        UUID contentId = UUID.randomUUID();
        given(contentStatsRepository.applyStatDelta(contentId, -4.0, -1)).willReturn(1);

        // when
        contentStatService.reviewUpdateContentStat(contentId, -4.0, -1);

        // then
        verify(contentStatsRepository).applyStatDelta(contentId, -4.0, -1);
    }
}
