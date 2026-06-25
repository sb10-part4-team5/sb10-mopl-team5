package com.codeit.team5.mopl.binarycontent;

import java.util.Set;
import java.util.UUID;
import org.springframework.util.StringUtils;

public interface BinaryContentStorage {

    Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    default String generateKey(UUID contentId, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String normalizedExt = (extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase()))
                ? "." + extension.toLowerCase()
                : "";
        return "thumbnails/" + contentId + "/" + UUID.randomUUID() + normalizedExt;
    }

    String toUrl(String key);

    void store(String key, byte[] bytes);
}
