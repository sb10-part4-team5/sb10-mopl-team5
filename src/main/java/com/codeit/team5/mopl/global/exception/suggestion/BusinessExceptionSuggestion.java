package com.codeit.team5.mopl.global.exception.suggestion;

import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BusinessExceptionSuggestion extends RuntimeException {

    private final HttpStatus status;
    private final String exceptionType;
    private final String message; // client 에게 보낼 message
    private String detailMessage; // logging 을 위한 message

    public BusinessExceptionSuggestion(ErrorCodeSuggestion errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
        this.exceptionType = this.getClass().getSimpleName();
        this.message = errorCode.getMessage();
        this.detailMessage = this.message;
    }

    public BusinessExceptionSuggestion(ErrorCodeSuggestion errorCode, Map<String, Object> details) {
        this(errorCode);
        this.detailMessage = details.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue().toString())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.exceptionType + "[", "]")
                .add("HttpStatus=" + status)
                .add("detailMessage='" + detailMessage + "'")
                .toString();
    }
}
