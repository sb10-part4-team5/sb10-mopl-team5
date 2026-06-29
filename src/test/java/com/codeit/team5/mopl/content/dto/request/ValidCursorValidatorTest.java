package com.codeit.team5.mopl.content.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.codeit.team5.mopl.content.entity.ContentSortByType;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class ValidCursorValidatorTest {

    private ValidCursorValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new ValidCursorValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    private ContentCursorRequest request(String cursor, String idAfter, ContentSortByType sortBy) {
        return new ContentCursorRequest(null, null, null, cursor, idAfter, 20, Sort.Direction.DESC, sortBy);
    }

    // --- 커서 없음 (첫 페이지) ---
    @Test
    @DisplayName("cursor와 idAfter 모두 null이면 유효하다")
    void valid_bothNull() {
        assertThat(validator.isValid(request(null, null, ContentSortByType.CREATED_AT), context)).isTrue();
    }

    // --- pair 검증 ---
    @Test
    @DisplayName("cursor만 있고 idAfter가 null이면 유효하지 않다")
    void invalid_onlyCursor() {
        assertThat(validator.isValid(request(Instant.now().toString(), null, ContentSortByType.CREATED_AT), context)).isFalse();
    }

    @Test
    @DisplayName("idAfter만 있고 cursor가 null이면 유효하지 않다")
    void invalid_onlyIdAfter() {
        assertThat(validator.isValid(request(null, UUID.randomUUID().toString(), ContentSortByType.CREATED_AT), context)).isFalse();
    }

    // --- idAfter UUID 검증 ---
    @Test
    @DisplayName("idAfter가 UUID 형식이 아니면 유효하지 않다")
    void invalid_idAfterNotUuid() {
        assertThat(validator.isValid(request(Instant.now().toString(), "not-a-uuid", ContentSortByType.CREATED_AT), context)).isFalse();
    }

    // --- CREATED_AT cursor 검증 ---
    @Test
    @DisplayName("CREATED_AT 정렬에서 유효한 ISO-8601 cursor는 유효하다")
    void valid_createdAt_cursor() {
        String cursor = Instant.now().toString();
        String idAfter = UUID.randomUUID().toString();
        assertThat(validator.isValid(request(cursor, idAfter, ContentSortByType.CREATED_AT), context)).isTrue();
    }

    @Test
    @DisplayName("CREATED_AT 정렬에서 날짜 형식이 아닌 cursor는 유효하지 않다")
    void invalid_createdAt_cursor() {
        assertThat(validator.isValid(request("not-a-date", UUID.randomUUID().toString(), ContentSortByType.CREATED_AT), context)).isFalse();
    }

    // --- WATCHER_COUNT cursor 검증 ---
    @Test
    @DisplayName("WATCHER_COUNT 정렬에서 유효한 숫자 cursor는 유효하다")
    void valid_watcherCount_cursor() {
        assertThat(validator.isValid(request("100", UUID.randomUUID().toString(), ContentSortByType.WATCHER_COUNT), context)).isTrue();
    }

    @Test
    @DisplayName("WATCHER_COUNT 정렬에서 숫자가 아닌 cursor는 유효하지 않다")
    void invalid_watcherCount_cursor() {
        assertThat(validator.isValid(request("abc", UUID.randomUUID().toString(), ContentSortByType.WATCHER_COUNT), context)).isFalse();
    }

    // --- RATE cursor 검증 ---
    @Test
    @DisplayName("RATE 정렬에서 유효한 소수 cursor는 유효하다")
    void valid_rate_cursor() {
        assertThat(validator.isValid(request("4.5", UUID.randomUUID().toString(), ContentSortByType.RATE), context)).isTrue();
    }

    @Test
    @DisplayName("RATE 정렬에서 숫자가 아닌 cursor는 유효하지 않다")
    void invalid_rate_cursor_notNumber() {
        assertThat(validator.isValid(request("abc", UUID.randomUUID().toString(), ContentSortByType.RATE), context)).isFalse();
    }

    @Test
    @DisplayName("RATE 정렬에서 NaN cursor는 유효하지 않다")
    void invalid_rate_cursor_nan() {
        assertThat(validator.isValid(request("NaN", UUID.randomUUID().toString(), ContentSortByType.RATE), context)).isFalse();
    }

    @Test
    @DisplayName("RATE 정렬에서 Infinity cursor는 유효하지 않다")
    void invalid_rate_cursor_infinity() {
        assertThat(validator.isValid(request("Infinity", UUID.randomUUID().toString(), ContentSortByType.RATE), context)).isFalse();
    }
}
