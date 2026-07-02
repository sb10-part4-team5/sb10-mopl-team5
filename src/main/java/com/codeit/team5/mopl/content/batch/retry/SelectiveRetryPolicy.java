package com.codeit.team5.mopl.content.batch.retry;

import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;

/**
 * 재시도해서 성공할 가능성이 있는 예외만 재시도 대상으로 판단하는 정책.
 * 판단 기준은 {@link ExternalFailureClassifier}를 따른다 (스킵 정책과 공유).
 */
public class SelectiveRetryPolicy extends SimpleRetryPolicy {

    public SelectiveRetryPolicy(int maxAttempts) {
        super(maxAttempts);
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        if (t == null) {
            return true;
        }
        return ExternalFailureClassifier.isTransientFailure(t) && super.canRetry(context);
    }
}
