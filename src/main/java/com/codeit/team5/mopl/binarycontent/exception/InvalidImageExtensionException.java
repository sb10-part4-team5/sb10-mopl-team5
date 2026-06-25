package com.codeit.team5.mopl.binarycontent.exception;

import com.codeit.team5.mopl.global.exception.suggestion.BusinessExceptionSuggestion;
import org.springframework.http.HttpStatus;

public class InvalidImageExtensionException extends BusinessExceptionSuggestion {

    public InvalidImageExtensionException(String extension) {
        super(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 확장자입니다: " + extension);
    }
}
