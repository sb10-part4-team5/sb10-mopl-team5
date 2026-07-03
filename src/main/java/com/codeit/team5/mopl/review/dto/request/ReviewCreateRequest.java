package com.codeit.team5.mopl.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "ReviewCreateRequest", description = "리뷰 생성 요청")
public record ReviewCreateRequest(
    @Schema(description = "콘텐츠 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "contentId는 필수입니다.")
    UUID contentId,

    @Schema(description = "리뷰 내용", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "리뷰 내용은 필수입니다.")
    String text,

    @Schema(description = "평점 (0.0 ~ 5.0)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "rating은 필수입니다.")
    @DecimalMin(value = "0.0", message = "평점은 0.0 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "평점은 5.0 이하여야 합니다.")
    Double rating
) {
}
