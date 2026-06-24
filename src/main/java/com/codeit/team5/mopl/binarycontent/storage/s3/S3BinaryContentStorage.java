package com.codeit.team5.mopl.binarycontent.storage.s3;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3BinaryContentStorage implements BinaryContentStorage {

    private final S3StorageProperties properties;

    public S3BinaryContentStorage(S3StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateKey(UUID contentId, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String filename = UUID.randomUUID() + (extension != null ? "." + extension : "");
        return "thumbnails/" + contentId + "/" + filename;
    }

    @Override
    public String toUrl(String key) {
        return "https://" + properties.bucketName() + ".s3." + properties.region() + ".amazonaws.com/" + key;
    }

    @Override
    public void store(String key, byte[] bytes) {
        throw new UnsupportedOperationException("S3 스토리지가 아직 구현되지 않았습니다.");
    }
}
