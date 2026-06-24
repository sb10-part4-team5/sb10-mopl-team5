package com.codeit.team5.mopl.binarycontent.exception;

import java.nio.file.Path;
import org.springframework.http.HttpStatus;

public class UploadDirectoryInitException extends BinaryContentStorageException {

    public UploadDirectoryInitException(Path uploadDir) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 디렉토리 생성 실패: " + uploadDir);
    }
}
