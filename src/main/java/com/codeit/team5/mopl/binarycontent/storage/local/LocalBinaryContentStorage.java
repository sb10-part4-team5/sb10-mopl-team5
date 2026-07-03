package com.codeit.team5.mopl.binarycontent.storage.local;

import com.codeit.team5.mopl.binarycontent.storage.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.exception.FileStorageException;
import com.codeit.team5.mopl.binarycontent.exception.UploadDirectoryInitException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "mopl.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalBinaryContentStorage implements BinaryContentStorage {

    private final Path uploadDir;
    private final String baseUrl;

    public LocalBinaryContentStorage(LocalStorageProperties properties) {
        this.uploadDir = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
        this.baseUrl = properties.baseUrl();
        init();
    }

    @Override
    public String toUrl(String key) {
        return baseUrl + "/" + key;
    }

    @Override
    public void store(String key, byte[] bytes, String contentType) {
        Path destination = uploadDir.resolve(key);
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, bytes);
        } catch (IOException e) {
            throw new FileStorageException(key, e);
        }
        log.debug("파일 저장 완료: {}", destination);
    }

    @Override
    public void delete(String key) {
        Path destination = uploadDir.resolve(key);
        try {
            Files.deleteIfExists(destination);
        } catch (IOException e) {
            throw new FileStorageException(key, e);
        }
        log.debug("파일 삭제 완료: {}", destination);
    }

    private void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new UploadDirectoryInitException(uploadDir, e);
        }
    }
}
