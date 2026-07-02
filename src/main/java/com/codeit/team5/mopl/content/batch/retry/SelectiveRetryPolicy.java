package com.codeit.team5.mopl.content.batch.retry;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 재시도해서 성공할 가능성이 있는 예외만 재시도 대상으로 판단하는 정책.
 * <p>
 * - 네트워크 자체 실패(WebClientRequestException): 항상 재시도 (응답을 못 받았으므로 서버 상태 판단 불가)
 * - DB 일시적 오류(TransientDataAccessException): 항상 재시도
 * - 5xx 응답(WebClientResponseException): 재시도 (서버 측 일시적 문제일 가능성)
 * - 429 응답: 재시도 (레이트 리밋 — 잠시 후 재요청하면 성공할 가능성이 있음)
 * - 그 외 4xx 응답: 재시도하지 않음 (요청 자체가 잘못됐으므로 재시도해도 결과가 같음)
 */
public class SelectiveRetryPolicy extends SimpleRetryPolicy {

    private static final int TOO_MANY_REQUESTS = 429;

    public SelectiveRetryPolicy(int maxAttempts) {
        super(maxAttempts);
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        if (t == null) {
            return true;
        }
        boolean retryable = (t instanceof WebClientRequestException)
                || (t instanceof TransientDataAccessException)
                || (t instanceof WebClientResponseException e && isRetryableStatus(e.getStatusCode()));
        return retryable && super.canRetry(context);
    }

    private boolean isRetryableStatus(HttpStatusCode status) {
        return status.is5xxServerError() || status.value() == TOO_MANY_REQUESTS;
    }
}
