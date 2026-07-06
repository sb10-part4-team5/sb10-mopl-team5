package com.codeit.team5.mopl.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Schema(name = "ReviewUpdateRequest", description = "리뷰 수정 요청")
public record ReviewUpdateRequest(
    @Schema(description = "리뷰 내용")
    String text,

    @Schema(description = "평점 (0.0 ~ 5.0)")
    @DecimalMin(value = "0.0", message = "평점은 0.0 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "평점은 5.0 이하여야 합니다.")
    Double rating
) {
}
