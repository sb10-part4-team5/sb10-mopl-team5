package com.codeit.team5.mopl.binarycontent.support;

import com.codeit.team5.mopl.binarycontent.ImageExtension;
import com.codeit.team5.mopl.binarycontent.exception.BinaryContentStorageException;
import com.codeit.team5.mopl.binarycontent.exception.InvalidImageExtensionException;
import com.codeit.team5.mopl.global.dto.FileRequest;
import java.io.IOException;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * MultipartFile(web 타입)을 서비스 계층용 {@link FileRequest}로 변환한다.
 * 변환 시점에 확장자를 검증(fail-fast)하고 바이트를 읽어, web/IO 의존을 컨트롤러 경계에서 차단한다.
 */
@UtilityClass
public class MultipartFiles {

    public FileRequest toImageResource(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ImageExtension.from(extension).isEmpty()) {
            throw new InvalidImageExtensionException(extension);
        }
        try {
            return new FileRequest(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new BinaryContentStorageException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일을 읽을 수 없습니다: " + file.getOriginalFilename(), e);
        }
    }
}
