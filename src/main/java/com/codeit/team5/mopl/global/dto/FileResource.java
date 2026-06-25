package com.codeit.team5.mopl.global.dto;

/**
 * 업로드 파일을 웹 계층(MultipartFile)에서 분리한 순수 데이터 홀더.
 * 서비스 계층이 web 타입에 의존하지 않도록 컨트롤러에서 변환하여 전달한다.
 */
public record FileResource(
        byte[] bytes,
        String filename,
        String contentType
) {
}
