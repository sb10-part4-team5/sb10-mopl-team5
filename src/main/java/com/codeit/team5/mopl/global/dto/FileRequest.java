package com.codeit.team5.mopl.global.dto;

import java.util.Arrays;
import java.util.Objects;

/**
 * 업로드 파일을 웹 계층(MultipartFile)에서 분리한 순수 데이터 홀더.
 * 서비스 계층이 web 타입에 의존하지 않도록 컨트롤러에서 변환하여 전달한다.
 * Content-Type은 클라이언트 값을 신뢰하지 않고 저장 시점에 확장자로 결정한다.
 */
public record FileRequest(
        byte[] bytes,
        String filename
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileRequest other)) {
            return false;
        }
        return Arrays.equals(bytes, other.bytes) && Objects.equals(filename, other.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(bytes), filename);
    }

    @Override
    public String toString() {
        return "FileRequest{filename='" + filename + "', size="
                + (bytes != null ? bytes.length : 0) + "}";
    }
}
