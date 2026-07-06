package com.codeit.team5.mopl.binarycontent.storage.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.binarycontent.exception.BinaryContentStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3BinaryContentStorageTest {

    @Mock
    private S3Client s3Client;

    private S3BinaryContentStorage storage;

    @BeforeEach
    void setUp() {
        S3StorageProperties properties =
                new S3StorageProperties(null, null, "mopl-bucket", "ap-northeast-2", "https://cdn.mopl-dev.site", null, null);
        storage = new S3BinaryContentStorage(properties, s3Client);
    }

    @Test
    @DisplayName("S3 객체 업로드 성공")
    void store_success() {
        // When
        storage.store("profiles/abc.jpg", new byte[]{1, 2, 3}, "image/jpeg");

        // Then
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("mopl-bucket");
        assertThat(request.key()).isEqualTo("profiles/abc.jpg");
        assertThat(request.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("SDK 예외가 발생하면 BinaryContentStorageException으로 변환하며 S3 업로드 실패")
    void store_sdkException_throwsBinaryContentStorageException() {
        // Given
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willThrow(SdkClientException.create("S3 연결 실패"));

        // When & Then
        assertThatThrownBy(() -> storage.store("profiles/abc.jpg", new byte[]{1, 2, 3}, "image/jpeg"))
                .isInstanceOf(BinaryContentStorageException.class);
    }

    @Test
    @DisplayName("S3 URL 생성 성공")
    void toUrl_success() {
        // When
        String url = storage.toUrl("profiles/abc.jpg");

        // Then
        assertThat(url).isEqualTo("https://cdn.mopl-dev.site/profiles/abc.jpg");
    }

    @Test
    @DisplayName("S3 객체 삭제 성공")
    void delete_success() {
        // When
        storage.delete("profiles/abc.jpg");

        // Then
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        DeleteObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("mopl-bucket");
        assertThat(request.key()).isEqualTo("profiles/abc.jpg");
    }

    @Test
    @DisplayName("삭제 중 SDK 예외가 발생하면 BinaryContentStorageException으로 변환하며 S3 삭제 실패")
    void delete_sdkException_throwsBinaryContentStorageException() {
        // Given
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .willThrow(SdkClientException.create("S3 연결 실패"));

        // When & Then
        assertThatThrownBy(() -> storage.delete("profiles/abc.jpg"))
                .isInstanceOf(BinaryContentStorageException.class);
    }
}
