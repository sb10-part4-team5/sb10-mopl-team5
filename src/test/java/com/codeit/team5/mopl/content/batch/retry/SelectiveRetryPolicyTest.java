package com.codeit.team5.mopl.content.batch.retry;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryContext;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class SelectiveRetryPolicyTest {

    private final SelectiveRetryPolicy policy = new SelectiveRetryPolicy(3);

    private RetryContext contextWith(Throwable t) {
        RetryContextSupport context = new RetryContextSupport(null);
        context.registerThrowable(t);
        return context;
    }

    @Test
    @DisplayName("네트워크 자체 실패는 재시도한다")
    void canRetry_webClientRequestException_returnsTrue() {
        // given
        WebClientRequestException e = new WebClientRequestException(
                new RuntimeException("connect timeout"), HttpMethod.GET, URI.create("http://test"), new HttpHeaders());

        // when, then
        assertThat(policy.canRetry(contextWith(e))).isTrue();
    }

    @Test
    @DisplayName("DB 일시적 오류는 재시도한다")
    void canRetry_transientDataAccessException_returnsTrue() {
        // given
        QueryTimeoutException e = new QueryTimeoutException("timeout");

        // when, then
        assertThat(policy.canRetry(contextWith(e))).isTrue();
    }

    @Test
    @DisplayName("5xx 응답은 재시도한다")
    void canRetry_5xxResponse_returnsTrue() {
        // given
        WebClientResponseException e = WebClientResponseException.create(500, "Internal Server Error", null, null, null);

        // when, then
        assertThat(policy.canRetry(contextWith(e))).isTrue();
    }

    @Test
    @DisplayName("429 응답은 재시도한다")
    void canRetry_429_returnsTrue() {
        // given
        WebClientResponseException e = WebClientResponseException.create(429, "Too Many Requests", null, null, null);

        // when, then
        assertThat(policy.canRetry(contextWith(e))).isTrue();
    }

    @Test
    @DisplayName("429가 아닌 4xx 응답은 재시도하지 않는다")
    void canRetry_4xxOtherThan429_returnsFalse() {
        // given
        WebClientResponseException e = WebClientResponseException.create(400, "Bad Request", null, null, null);

        // when, then
        assertThat(policy.canRetry(contextWith(e))).isFalse();
    }

    @Test
    @DisplayName("재시도 대상 예외라도 maxAttempts를 초과하면 재시도하지 않는다")
    void canRetry_maxAttemptsExceeded_returnsFalse() {
        // given
        RetryContextSupport context = new RetryContextSupport(null);
        WebClientResponseException e = WebClientResponseException.create(503, "Service Unavailable", null, null, null);
        context.registerThrowable(e); // 1회
        context.registerThrowable(e); // 2회
        context.registerThrowable(e); // 3회 (maxAttempts=3 도달)

        // when, then
        assertThat(policy.canRetry(context)).isFalse();
    }
}
