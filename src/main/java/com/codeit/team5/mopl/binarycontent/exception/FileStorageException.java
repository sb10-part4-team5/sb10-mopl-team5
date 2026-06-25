package com.codeit.team5.mopl.binarycontent.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends BinaryContentStorageException {

    public FileStorageException(String key) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패: " + key);
    }

    public FileStorageException(String key, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패: " + key, cause);
    }
}
