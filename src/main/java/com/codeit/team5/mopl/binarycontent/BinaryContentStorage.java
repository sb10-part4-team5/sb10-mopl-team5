package com.codeit.team5.mopl.binarycontent;

import com.codeit.team5.mopl.binarycontent.exception.InvalidImageExtensionException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.util.StringUtils;

public interface BinaryContentStorage {

    default String generateKey(StorageDirectory directory, UUID ownerId, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String normalizedExt = ImageExtension.from(extension)
                .map(e -> "." + e.name().toLowerCase(Locale.ROOT))
                .orElse("");
        return directory.value() + "/" + ownerId + "/" + UUID.randomUUID() + normalizedExt;
    }

    default ImageExtension validateImageKey(String key) {
        String extension = StringUtils.getFilenameExtension(key);
        return ImageExtension.from(extension)
                .orElseThrow(() -> new InvalidImageExtensionException(extension));
    }

    String toUrl(String key);

    void store(String key, byte[] bytes);
}
