package com.codeit.team5.mopl.binarycontent;

import java.util.UUID;
import org.springframework.util.StringUtils;

public interface BinaryContentStorage {

    default String generateKey(UUID contentId, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String filename = UUID.randomUUID() + (extension != null ? "." + extension : "");
        return "thumbnails/" + contentId + "/" + filename;
    }

    String toUrl(String key);

    void store(String key, byte[] bytes);
}
