package com.codeit.team5.mopl.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,}$",
                message = "비밀번호는 영문과 숫자를 포함하여 8자 이상이어야 합니다."
        )
        String password
) {
}
