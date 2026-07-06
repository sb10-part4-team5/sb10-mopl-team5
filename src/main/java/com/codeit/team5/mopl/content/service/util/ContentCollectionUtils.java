package com.codeit.team5.mopl.content.service.util;

import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.content.entity.Content;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public final class ContentCollectionUtils {

    private ContentCollectionUtils() {}

    public static Instant parseDate(String date, String logPrefix) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            log.warn("[{}] 날짜 파싱 실패 - date={}", logPrefix, date);
            return null;
        }
    }

    public static void attachThumbnail(Content content, String url,
                                       BinaryContentRepository binaryContentRepository,
                                       String baseUrl) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        BinaryContent thumbnail = binaryContentRepository.save(
                BinaryContent.of(baseUrl + url)
        );
        content.attachThumbnail(thumbnail);
    }
}
