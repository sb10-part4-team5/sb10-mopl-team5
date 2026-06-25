package com.codeit.team5.mopl.binarycontent.storage.s3;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.exception.BinaryContentStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@ConditionalOnProperty(name = "mopl.storage.type", havingValue = "s3")
public class S3BinaryContentStorage implements BinaryContentStorage {

    private final S3StorageProperties properties;
    private final S3Client s3Client;

    public S3BinaryContentStorage(S3StorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @Override
    public String toUrl(String key) {
        return "https://" + properties.bucketName() + ".s3." + properties.region() + ".amazonaws.com/" + key;
    }

    @Override
    public void store(String key, byte[] bytes) {
        String contentType = validateImageKey(key).contentType();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucketName())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes));
            log.debug("S3 업로드 완료: key={}, contentType={}", key, contentType);
        } catch (SdkException e) {
            throw new BinaryContentStorageException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "S3 업로드 실패: key=" + key, e);
        }
    }
}
