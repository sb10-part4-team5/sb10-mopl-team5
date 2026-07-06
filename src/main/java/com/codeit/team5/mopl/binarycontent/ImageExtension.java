package com.codeit.team5.mopl.binarycontent;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 허용 이미지 확장자와 그에 대응하는 Content-Type을 한 곳에서 관리한다.
 * 확장자를 추가하려면 이 enum에 상수만 추가하면 된다.
 */
public enum ImageExtension {
    JPG("image/jpeg"),
    JPEG("image/jpeg"),
    PNG("image/png"),
    GIF("image/gif"),
    WEBP("image/webp");

    private final String contentType;

    ImageExtension(String contentType) {
        this.contentType = contentType;
    }

    public String contentType() {
        return contentType;
    }

    public static Optional<ImageExtension> from(String extension) {
        if (extension == null || extension.isBlank()) {
            return Optional.empty();
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(e -> e.name().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }
}
