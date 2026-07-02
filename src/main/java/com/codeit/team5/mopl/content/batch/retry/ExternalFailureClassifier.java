package com.codeit.team5.mopl.content.batch.retry;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 외부 API/DB 호출 실패가 재시도해서 성공할 가능성이 있는(=시스템 전역/일시적) 문제인지 판단한다.
 * <p>
 * 이 판단은 재시도 정책({@link SelectiveRetryPolicy})과 스킵 정책(SelectiveSkipPolicy) 양쪽에서
 * 공유된다 — 재시도해도 가망 없는 오류를 스킵 대상으로, 재시도까지 실패한 시스템 전역 문제는
 * 스킵하지 않고 Step을 즉시 실패시키기 위해서다.
 * <p>
 * - 네트워크 자체 실패(WebClientRequestException): 항상 해당 (응답을 못 받았으므로 서버 상태 판단 불가)
 * - DB 일시 오류(TransientDataAccessException): 항상 해당
 * - 5xx 응답(WebClientResponseException): 해당 (서버 측 일시적 문제일 가능성)
 * - 429 응답: 해당 (레이트 리밋 — 잠시 후 재요청하면 성공할 가능성이 있음)
 * - 그 외 4xx 응답: 해당하지 않음 (요청 자체가 잘못됐거나 대상이 없는 등 이 항목만의 문제)
 */
public final class ExternalFailureClassifier {

    private ExternalFailureClassifier() {
    }

    public static boolean isTransientFailure(Throwable t) {
        if (t == null) {
            return false;
        }
        return (t instanceof WebClientRequestException)
                || (t instanceof TransientDataAccessException)
                || (t instanceof WebClientResponseException e && isRetryableStatus(e.getStatusCode()));
    }

    private static boolean isRetryableStatus(HttpStatusCode status) {
        return status.is5xxServerError() || status.value() == HttpStatus.TOO_MANY_REQUESTS.value();
    }
}
