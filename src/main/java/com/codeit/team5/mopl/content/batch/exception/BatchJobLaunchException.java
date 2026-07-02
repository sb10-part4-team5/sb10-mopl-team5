package com.codeit.team5.mopl.content.batch.exception;

import com.codeit.team5.mopl.content.exception.ContentException;
import org.springframework.http.HttpStatus;

public class BatchJobLaunchException extends ContentException {

    public BatchJobLaunchException(String jobName, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "배치 작업(" + jobName + ") 실행에 실패했습니다.");
        initCause(cause);
    }
}
