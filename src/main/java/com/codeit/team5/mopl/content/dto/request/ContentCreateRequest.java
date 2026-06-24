package com.codeit.team5.mopl.content.dto.request;

import com.codeit.team5.mopl.content.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ContentCreateRequest(
        @NotNull(message = "콘텐츠 타입은 필수입니다.")
        ContentType type,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 500, message = "제목은 500자를 초과할 수 없습니다.")
        String title,

        String description,

        @NotNull(message = "콘텐츠 태그는 필수 입니다.")
        @NotEmpty(message = "콘텐츠 태그 목록은 비어있을 수 없습니다.")
        List<@NotBlank(message = "태그는 공백일 수 없습니다.") String> tags
) {
}
