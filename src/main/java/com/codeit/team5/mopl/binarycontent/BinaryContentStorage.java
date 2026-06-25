package com.codeit.team5.mopl.binarycontent;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.StringUtils;

public interface BinaryContentStorage {

    Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    default String generateKey(UUID contentId, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String lowerExt = extension != null ? extension.toLowerCase(Locale.ROOT) : null;
        String normalizedExt = (lowerExt != null && ALLOWED_EXTENSIONS.contains(lowerExt))
                ? "." + lowerExt
                : "";
        return "thumbnails/" + contentId + "/" + UUID.randomUUID() + normalizedExt;
    }

    String toUrl(String key);

    void store(String key, byte[] bytes);
}
