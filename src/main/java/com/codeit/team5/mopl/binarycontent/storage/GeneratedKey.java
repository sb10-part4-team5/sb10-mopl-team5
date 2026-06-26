package com.codeit.team5.mopl.binarycontent.storage;

/**
 * 저장소 키와 그에 대응하는 Content-Type을 함께 전달하는 값 객체.
 */
public record GeneratedKey(
        String key,
        String contentType
) {
}
