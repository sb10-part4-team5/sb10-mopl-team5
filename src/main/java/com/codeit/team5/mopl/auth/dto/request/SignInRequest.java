package com.codeit.team5.mopl.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        String username,        // email
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {

}
