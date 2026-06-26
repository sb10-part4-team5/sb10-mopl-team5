package com.codeit.team5.mopl.binarycontent.storage;

import com.codeit.team5.mopl.binarycontent.ImageExtension;
import com.codeit.team5.mopl.binarycontent.exception.InvalidImageExtensionException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 저장소 키 생성 책임을 담당한다.
 * 확장자 검증 → 폴더 prefix/소유자/난수 조합 → Content-Type 결정을 한 곳에서 처리한다.
 */
@Component
public class StorageKeyFactory {

    public GeneratedKey generate(StorageDirectory directory, UUID ownerId, String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        ImageExtension imageExtension = ImageExtension.from(extension)
                .orElseThrow(() -> new InvalidImageExtensionException(extension));
        String key = directory.value() + "/" + ownerId + "/" + UUID.randomUUID()
                + "." + imageExtension.name().toLowerCase(Locale.ROOT);
        return new GeneratedKey(key, imageExtension.contentType());
    }
}
