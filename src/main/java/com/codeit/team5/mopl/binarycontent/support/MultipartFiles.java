package com.codeit.team5.mopl.binarycontent.support;

import com.codeit.team5.mopl.binarycontent.ImageExtension;
import com.codeit.team5.mopl.binarycontent.exception.InvalidImageExtensionException;
import com.codeit.team5.mopl.global.dto.FileResource;
import java.io.IOException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * MultipartFile(web 타입)을 서비스 계층용 {@link FileResource}로 변환한다.
 * 변환 시점에 확장자를 검증(fail-fast)하고 바이트를 읽어, web/IO 의존을 컨트롤러 경계에서 차단한다.
 */
@Slf4j
@UtilityClass
public class MultipartFiles {

    public FileResource toImageResource(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ImageExtension.from(extension).isEmpty()) {
            throw new InvalidImageExtensionException(extension);
        }
        try {
            return new FileResource(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            log.warn("파일 바이트 읽기 실패: filename={}", file.getOriginalFilename(), e);
            return null;
        }
    }
}
