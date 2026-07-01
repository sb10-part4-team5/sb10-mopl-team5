package com.codeit.team5.mopl.sse.exception;

import com.codeit.team5.mopl.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SseMissedEventSendFailException extends BusinessException {

    public SseMissedEventSendFailException() {

        super(HttpStatus.INTERNAL_SERVER_ERROR, "놓쳤던 SSE 이벤트 전송이 실패했습니다.");
    }
}
