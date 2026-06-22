package com.codeit.team5.mopl.global.exception;

import java.util.Objects;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage != null ? detailMessage : Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }
}
