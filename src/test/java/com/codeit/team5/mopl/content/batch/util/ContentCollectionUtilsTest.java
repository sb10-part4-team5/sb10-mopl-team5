package com.codeit.team5.mopl.content.batch.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentCollectionUtilsTest {

    @Mock
    private BinaryContentRepository binaryContentRepository;

    // --- parseDate ---

    @Test
    @DisplayName("정상적인 날짜 문자열은 UTC Instant로 변환된다")
    void parseDate_validDate_returnsInstant() {
        // when
        Instant result = ContentCollectionUtils.parseDate("2023-08-12", "TEST");

        // then
        Instant expected = LocalDate.of(2023, 8, 12).atStartOfDay(ZoneOffset.UTC).toInstant();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("날짜가 null이면 null을 반환한다")
    void parseDate_nullDate_returnsNull() {
        // when, then
        assertThat(ContentCollectionUtils.parseDate(null, "TEST")).isNull();
    }

    @Test
    @DisplayName("날짜가 빈 문자열이면 null을 반환한다")
    void parseDate_blankDate_returnsNull() {
        // when, then
        assertThat(ContentCollectionUtils.parseDate("   ", "TEST")).isNull();
    }

    @Test
    @DisplayName("날짜 형식이 잘못된 경우 null을 반환한다")
    void parseDate_invalidFormat_returnsNull() {
        // when, then
        assertThat(ContentCollectionUtils.parseDate("not-a-date", "TEST")).isNull();
    }

    // --- attachThumbnail ---

    @Test
    @DisplayName("URL이 있으면 baseUrl을 붙여 BinaryContent를 저장하고 콘텐츠에 첨부한다")
    void attachThumbnail_validUrl_savesAndAttaches() {
        // given
        Content content = mockContent();
        BinaryContent thumbnail = BinaryContent.completed("https://base.url/poster.jpg");
        ArgumentCaptor<BinaryContent> captor = ArgumentCaptor.forClass(BinaryContent.class);
        given(binaryContentRepository.save(captor.capture())).willReturn(thumbnail);

        // when
        ContentCollectionUtils.attachThumbnail(content, "/poster.jpg", binaryContentRepository, "https://base.url");

        // then
        assertThat(captor.getValue().getUrl()).isEqualTo("https://base.url/poster.jpg");
        assertThat(content.getThumbnail()).isEqualTo(thumbnail);
    }

    @Test
    @DisplayName("baseUrl이 빈 문자열이면 URL을 그대로 사용한다")
    void attachThumbnail_emptyBaseUrl_usesUrlAsIs() {
        // given
        Content content = mockContent();
        BinaryContent thumbnail = BinaryContent.completed("https://thumb.url/img.jpg");
        ArgumentCaptor<BinaryContent> captor = ArgumentCaptor.forClass(BinaryContent.class);
        given(binaryContentRepository.save(captor.capture())).willReturn(thumbnail);

        // when
        ContentCollectionUtils.attachThumbnail(content, "https://thumb.url/img.jpg", binaryContentRepository, "");

        // then
        assertThat(captor.getValue().getUrl()).isEqualTo("https://thumb.url/img.jpg");
    }

    @Test
    @DisplayName("URL이 null이면 저장하지 않는다")
    void attachThumbnail_nullUrl_doesNotSave() {
        // given
        Content content = mockContent();

        // when
        ContentCollectionUtils.attachThumbnail(content, null, binaryContentRepository, "https://base.url");

        // then
        verify(binaryContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("URL이 빈 문자열이면 저장하지 않는다")
    void attachThumbnail_blankUrl_doesNotSave() {
        // given
        Content content = mockContent();

        // when
        ContentCollectionUtils.attachThumbnail(content, "   ", binaryContentRepository, "https://base.url");

        // then
        verify(binaryContentRepository, never()).save(any());
    }

    private Content mockContent() {
        return Content.createByExternalSource(
                ContentType.MOVIE, "테스트", "설명",
                ContentSource.TMDB, "1", null, "{}"
        );
    }
}
