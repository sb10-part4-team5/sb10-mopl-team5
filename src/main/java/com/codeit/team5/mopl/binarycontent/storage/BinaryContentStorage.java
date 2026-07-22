package com.codeit.team5.mopl.binarycontent.storage;

import com.codeit.team5.mopl.binarycontent.exception.BinaryContentStorageException;
import org.springframework.http.HttpStatus;

public interface BinaryContentStorage {

    String toUrl(String key);

    void store(String key, byte[] bytes, String contentType);

    void delete(String key);

    String extractKey(String url);

    static String keyFromUrl(String url, String baseUrl) {
        String prefix = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        if (url == null || !url.startsWith(prefix)) {
            throw new BinaryContentStorageException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "URL에서 key를 추출할 수 없습니다: url=" + url);
        }
        String key = url.substring(prefix.length());
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }
}
