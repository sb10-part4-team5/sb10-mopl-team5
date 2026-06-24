package com.codeit.team5.mopl.binarycontent.storage.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.team5.mopl.binarycontent.exception.FileStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalBinaryContentStorageTest {

    @TempDir
    Path tempDir;

    private LocalBinaryContentStorage storage;

    @BeforeEach
    void setUp() {
        LocalStorageProperties properties = new LocalStorageProperties(
                "http://localhost:8080",
                tempDir.toString()
        );
        storage = new LocalBinaryContentStorage(properties);
    }

    @Test
    @DisplayName("generateKey는 thumbnails/{contentId}/{uuid}.{ext} 형식의 키를 반환한다")
    void generateKey_returnsCorrectFormat() {
        // given
        UUID contentId = UUID.randomUUID();

        // when
        String key = storage.generateKey(contentId, "test.jpg");

        // then
        assertThat(key).startsWith("thumbnails/" + contentId + "/");
        assertThat(key).endsWith(".jpg");
    }

    @Test
    @DisplayName("generateKey는 확장자가 없는 파일명도 처리한다")
    void generateKey_noExtension_returnsKeyWithoutExtension() {
        // given
        UUID contentId = UUID.randomUUID();

        // when
        String key = storage.generateKey(contentId, "testfile");

        // then
        assertThat(key).startsWith("thumbnails/" + contentId + "/");
        assertThat(key).doesNotContain(".");
    }

    @Test
    @DisplayName("toUrl은 baseUrl과 key를 조합한 URL을 반환한다")
    void toUrl_returnsCombinedUrl() {
        // given
        String key = "thumbnails/some-id/file.jpg";

        // when
        String url = storage.toUrl(key);

        // then
        assertThat(url).isEqualTo("http://localhost:8080/" + key);
    }

    @Test
    @DisplayName("store는 파일을 uploadDir 하위 경로에 저장한다")
    void store_savesFileToCorrectPath() throws IOException {
        // given
        UUID contentId = UUID.randomUUID();
        String key = storage.generateKey(contentId, "test.jpg");
        byte[] bytes = new byte[]{1, 2, 3};

        // when
        storage.store(key, bytes);

        // then
        Path saved = tempDir.resolve(key);
        assertThat(Files.exists(saved)).isTrue();
        assertThat(Files.readAllBytes(saved)).isEqualTo(bytes);
    }

    @Test
    @DisplayName("store는 중간 디렉토리가 없어도 자동으로 생성한다")
    void store_createsMissingDirectories() {
        // given
        String key = "thumbnails/" + UUID.randomUUID() + "/nested/file.jpg";
        byte[] bytes = new byte[]{1, 2, 3};

        // when
        storage.store(key, bytes);

        // then
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();
    }

    @Test
    @DisplayName("store 실패 시 FileStorageException을 던진다")
    void store_ioFailure_throwsFileStorageException() throws IOException {
        // given
        // key의 중간 경로 위치에 디렉토리 대신 파일을 미리 생성해서 createDirectories 실패 유도
        Path conflictPath = tempDir.resolve("thumbnails");
        Files.createFile(conflictPath); // 디렉토리여야 할 자리에 파일 생성

        String key = "thumbnails/test/file.jpg";
        byte[] bytes = new byte[]{1, 2, 3};

        // when & then
        assertThatThrownBy(() -> storage.store(key, bytes))
                .isInstanceOf(FileStorageException.class);
    }
}
