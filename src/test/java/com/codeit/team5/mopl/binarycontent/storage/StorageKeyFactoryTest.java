package com.codeit.team5.mopl.binarycontent.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.team5.mopl.binarycontent.exception.InvalidImageExtensionException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StorageKeyFactoryTest {

    private final StorageKeyFactory storageKeyFactory = new StorageKeyFactory();

    @Test
    @DisplayName("generate는 {prefix}/{ownerId}/{uuid}.{ext} 형식의 키와 contentType 반환 성공")
    void generate_returnsKeyAndContentType() {
        // given
        UUID ownerId = UUID.randomUUID();

        // when
        GeneratedKey generated = storageKeyFactory.generate(StorageDirectory.THUMBNAIL, ownerId, "test.jpg");

        // then
        assertThat(generated.key()).startsWith("thumbnails/" + ownerId + "/");
        assertThat(generated.key()).endsWith(".jpg");
        assertThat(generated.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("generate는 StorageDirectory에 맞는 prefix 적용 성공")
    void generate_appliesDirectoryPrefix() {
        // given
        UUID ownerId = UUID.randomUUID();

        // when
        GeneratedKey generated = storageKeyFactory.generate(StorageDirectory.PROFILE, ownerId, "test.png");

        // then
        assertThat(generated.key()).startsWith("profiles/" + ownerId + "/");
        assertThat(generated.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("generate는 대문자 확장자를 소문자로 정규화 성공")
    void generate_normalizesUppercaseExtension() {
        // given
        UUID ownerId = UUID.randomUUID();

        // when
        GeneratedKey generated = storageKeyFactory.generate(StorageDirectory.THUMBNAIL, ownerId, "photo.JPG");

        // then
        assertThat(generated.key()).endsWith(".jpg");
        assertThat(generated.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("generate는 확장자가 없는 파일명이면 예외 발생 실패")
    void generate_noExtension_throwsException() {
        // given
        UUID ownerId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> storageKeyFactory.generate(StorageDirectory.THUMBNAIL, ownerId, "testfile"))
                .isInstanceOf(InvalidImageExtensionException.class);
    }

    @Test
    @DisplayName("generate는 허용되지 않는 확장자이면 예외 발생 실패")
    void generate_disallowedExtension_throwsException() {
        // given
        UUID ownerId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> storageKeyFactory.generate(StorageDirectory.THUMBNAIL, ownerId, "malicious.exe"))
                .isInstanceOf(InvalidImageExtensionException.class);
    }
}
