package com.codeit.team5.mopl.content.batch.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class SelectiveSkipPolicyTest {

    private final SelectiveSkipPolicy policy = new SelectiveSkipPolicy(3);

    @Test
    @DisplayName("5xx 응답(시스템 전역 문제로 간주)은 스킵하지 않는다")
    void shouldSkip_5xxResponse_returnsFalse() throws Exception {
        // given
        WebClientResponseException e = WebClientResponseException.create(500, "Internal Server Error", null, null, null);

        // when, then
        assertThat(policy.shouldSkip(e, 0)).isFalse();
    }

    @Test
    @DisplayName("DB 일시적 오류(시스템 전역 문제로 간주)는 스킵하지 않는다")
    void shouldSkip_transientDataAccessException_returnsFalse() throws Exception {
        // given
        QueryTimeoutException e = new QueryTimeoutException("timeout");

        // when, then
        assertThat(policy.shouldSkip(e, 0)).isFalse();
    }

    @Test
    @DisplayName("429가 아닌 4xx 응답(대상 항목만의 문제)은 한도 내에서 스킵한다")
    void shouldSkip_4xxOtherThan429_returnsTrue() throws Exception {
        // given
        WebClientResponseException e = WebClientResponseException.create(404, "Not Found", null, null, null);

        // when, then
        assertThat(policy.shouldSkip(e, 0)).isTrue();
    }

    @Test
    @DisplayName("스킵 대상 예외가 skipLimit에 도달하면 SkipLimitExceededException을 던진다")
    void shouldSkip_limitExceeded_throwsException() {
        // given
        WebClientResponseException e = WebClientResponseException.create(404, "Not Found", null, null, null);

        // when, then
        assertThatThrownBy(() -> policy.shouldSkip(e, 3))
                .isInstanceOf(SkipLimitExceededException.class);
    }

    @Test
    @DisplayName("skipCount가 한도를 넘어도 시스템 전역 문제면 예외 없이 스킵 불가만 반환한다")
    void shouldSkip_limitExceededButTransientFailure_returnsFalseWithoutThrowing() throws Exception {
        // given
        WebClientResponseException e = WebClientResponseException.create(500, "Internal Server Error", null, null, null);

        // when, then
        assertThat(policy.shouldSkip(e, 3)).isFalse();
    }
}
