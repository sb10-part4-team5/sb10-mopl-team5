package com.codeit.team5.mopl.binarycontent.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import org.springframework.http.HttpStatus;

public class InvalidBinaryContentUrlException extends BusinessExceptionSuggestion {

    public InvalidBinaryContentUrlException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "BinaryContent url은 비어있을 수 없습니다.");
    }
}
