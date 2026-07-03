package com.codeit.team5.mopl.auth.controller.api;

import com.codeit.team5.mopl.auth.dto.request.ResetPasswordRequest;
import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "인증 관리")
public interface AuthApi {

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급합니다. "
                    + "Refresh Token은 HttpOnly Cookie로 응답됩니다.",
            security = {},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                            schema = @Schema(implementation = SignInRequest.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청(입력값 검증 실패)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "이메일 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))
            )
    })
    @PostMapping(
            path = "/api/auth/sign-in",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    default ResponseEntity<JwtResponse> login(
            @Valid @ModelAttribute SignInRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 사용자를 로그아웃합니다. "
                    + "서버에 저장된 Refresh Token을 무효화하고 Refresh Token Cookie를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))
            )
    })
    @PostMapping("/api/auth/sign-out")
    default ResponseEntity<Void> logout(
            @Parameter(description = "Refresh Token Cookie", required = false)
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(
            summary = "Access Token 재발급",
            description = "Refresh Token Cookie를 사용해 새로운 Access Token과 Refresh Token을 발급합니다. "
                    + "새 Refresh Token은 HttpOnly Cookie로 다시 응답됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token이 없거나 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))
            )
    })
    ResponseEntity<JwtResponse> refresh(
            @Parameter(description = "Refresh Token Cookie", required = false)
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
    );

    @Operation(
            summary = "CSRF Token 발급",
            description = "CSRF Token을 생성하고 응답 Cookie에 저장합니다. "
                    + "클라이언트는 이후 변경 요청 시 CSRF Token을 헤더에 포함해 요청합니다.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "CSRF Token 발급 성공"),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))
            )
    })
    ResponseEntity<Void> csrfToken(
            @Parameter(hidden = true) CsrfToken csrfToken
    );

    @Operation(
            summary = "비밀번호 초기화",
            description = "임시 비밀번호로 초기화 후 이메일로 전송합니다.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비밀번호 초기화 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    );
}
