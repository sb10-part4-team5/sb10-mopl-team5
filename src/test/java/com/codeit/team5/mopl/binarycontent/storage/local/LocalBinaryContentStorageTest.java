package com.codeit.team5.mopl.binarycontent.storage.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.team5.mopl.binarycontent.exception.FileStorageException;
import com.codeit.team5.mopl.binarycontent.exception.UploadDirectoryInitException;
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
        String key = "thumbnails/" + UUID.randomUUID() + "/test.jpg";
        byte[] bytes = new byte[]{1, 2, 3};

        // when
        storage.store(key, bytes, "image/jpeg");

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
        storage.store(key, bytes, "image/jpeg");

        // then
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();
    }

    @Test
    @DisplayName("uploadDir 자리에 파일이 존재하면 생성자에서 UploadDirectoryInitException을 던진다")
    void init_uploadDirIsFile_throwsUploadDirectoryInitException() throws IOException {
        // given
        Path filePath = tempDir.resolve("conflict-file");
        Files.createFile(filePath);

        LocalStorageProperties properties = new LocalStorageProperties(
                "http://localhost:8080",
                filePath.toString()
        );

        // when & then
        assertThatThrownBy(() -> new LocalBinaryContentStorage(properties))
                .isInstanceOf(UploadDirectoryInitException.class);
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
        assertThatThrownBy(() -> storage.store(key, bytes, "image/jpeg"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    @DisplayName("delete는 저장된 파일 삭제 성공")
    void delete_removesFile() {
        // given
        String key = "thumbnails/" + UUID.randomUUID() + "/test.jpg";
        storage.store(key, new byte[]{1, 2, 3}, "image/jpeg");
        assertThat(Files.exists(tempDir.resolve(key))).isTrue();

        // when
        storage.delete(key);

        // then
        assertThat(Files.exists(tempDir.resolve(key))).isFalse();
    }

    @Test
    @DisplayName("delete는 파일이 없어도 예외 없이 성공")
    void delete_missingFile_noException() {
        // given
        String key = "thumbnails/none/none.jpg";

        // when
        storage.delete(key);

        // then
        assertThat(Files.exists(tempDir.resolve(key))).isFalse();
    }
}
