package com.codeit.team5.mopl.content.batch.retry;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * 재시도까지 실패한 예외가 시스템 전역 문제({@link ExternalFailureClassifier})로 보이면 스킵하지 않고
 * Step을 즉시 실패시키고, 그 외(대상 항목만의 문제, 예: 영구적인 404)는 skipLimit 내에서 스킵한다.
 * <p>
 * noSkip(WebClientException.class)처럼 예외 타입만으로 뭉뚱그리면, 재시도 정책이 재시도 대상이
 * 아니라고 판단한 4xx까지도 스킵이 막혀 항목 하나 때문에 전체 배치가 중단된다. 이 정책은 재시도
 * 정책과 동일한 기준으로 판단해 그 불일치를 없앤다.
 */
public class SelectiveSkipPolicy implements SkipPolicy {

    private final int skipLimit;

    public SelectiveSkipPolicy(int skipLimit) {
        this.skipLimit = skipLimit;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
        if (ExternalFailureClassifier.isTransientFailure(t)) {
            return false;
        }
        if (skipCount >= skipLimit) {
            throw new SkipLimitExceededException(skipLimit, t);
        }
        return true;
    }
}
