package com.codeit.team5.mopl.binarycontent.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.team5.mopl.binarycontent.exception.InvalidImageExtensionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3BinaryContentStorageTest {

    @Mock
    private S3Client s3Client;

    private S3BinaryContentStorage storage;

    @BeforeEach
    void setUp() {
        S3StorageProperties properties =
                new S3StorageProperties(null, null, "mopl-bucket", "ap-northeast-2");
        storage = new S3BinaryContentStorage(properties, s3Client);
    }

    @Test
    @DisplayName("S3 객체 업로드 성공")
    void store_success() {
        // When
        storage.store("profiles/abc.jpg", new byte[]{1, 2, 3});

        // Then
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("허용되지 않은 확장자 업로드 실패")
    void store_invalidExtension_throwsException() {
        // When & Then
        assertThatThrownBy(() -> storage.store("profiles/malware.exe", new byte[]{1, 2, 3}))
                .isInstanceOf(InvalidImageExtensionException.class);

        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("S3 URL 생성 성공")
    void toUrl_success() {
        // When
        String url = storage.toUrl("profiles/abc.jpg");

        // Then
        assertThat(url)
                .isEqualTo("https://mopl-bucket.s3.ap-northeast-2.amazonaws.com/profiles/abc.jpg");
    }
}
