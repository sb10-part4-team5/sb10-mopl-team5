package com.codeit.team5.mopl.binarycontent.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class BinaryContentNotFoundException extends BusinessExceptionSuggestion {

    public BinaryContentNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "BinaryContent를 찾을 수 없습니다: " + id);
    }
}
