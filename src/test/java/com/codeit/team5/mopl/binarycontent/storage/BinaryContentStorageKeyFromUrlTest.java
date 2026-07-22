package com.codeit.team5.mopl.binarycontent.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.team5.mopl.binarycontent.exception.BinaryContentStorageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BinaryContentStorageKeyFromUrlTest {

    @Test
    @DisplayName("base + / + key 형태의 URL에서 key를 정확히 복원한다")
    void roundTrip() {
        String base = "https://cdn.example.com";
        String key = "profiles/abc.png";

        String url = base + "/" + key;

        assertThat(BinaryContentStorage.keyFromUrl(url, base)).isEqualTo(key);
    }

    @Test
    @DisplayName("baseUrl에 trailing slash가 있어도 key를 정확히 복원한다")
    void trailingSlashInBaseUrl() {
        assertThat(BinaryContentStorage.keyFromUrl(
                "https://cdn.example.com/profiles/abc.png", "https://cdn.example.com/"))
                .isEqualTo("profiles/abc.png");
    }

    @Test
    @DisplayName("중첩 경로 key도 복원한다")
    void nestedKey() {
        // given
        String url = "http://localhost:8080/files/thumbnails/2026/07/x.jpg";
        String base = "http://localhost:8080/files";

        // when
        String extracted = BinaryContentStorage.keyFromUrl(url, base);

        // then
        assertThat(extracted).isEqualTo("thumbnails/2026/07/x.jpg");
    }

    @Test
    @DisplayName("baseUrl prefix로 시작하지 않는 URL이면 예외를 던진다")
    void prefixMismatch() {
        assertThatThrownBy(() -> BinaryContentStorage.keyFromUrl(
                "https://other.com/profiles/abc.png", "https://cdn.example.com"))
                .isInstanceOf(BinaryContentStorageException.class);
    }

    @Test
    @DisplayName("url이 null이면 예외를 던진다")
    void nullUrl() {
        assertThatThrownBy(() -> BinaryContentStorage.keyFromUrl(null, "https://cdn.example.com"))
                .isInstanceOf(BinaryContentStorageException.class);
    }
}
