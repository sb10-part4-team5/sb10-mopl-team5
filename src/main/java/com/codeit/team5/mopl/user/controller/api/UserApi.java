package com.codeit.team5.mopl.user.controller.api;

import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용자 관리", description = "사용자 관련 API")
public interface UserApi {

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(입력값 검증 실패)"),
            @ApiResponse(responseCode = "409", description = "이메일 중복"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<UserResponse> registerUser(
            @Parameter(description = "회원가입 요청 본문", required = true)
            @Valid @RequestBody UserRegisterRequest userRegisterRequest);

    @Operation(summary = "사용자 상세 조회", description = "userId로 사용자 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "404", description = "해당 리소스 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<UserResponse> getUser(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable UUID userId);

    @Schema(name = "UserUpdateMultipartRequest")
    class UpdateMultipartRequest {
        @Schema(implementation = UserUpdateRequest.class)
        public UserUpdateRequest request;

        @Schema(type = "string", format = "binary", description = "프로필 이미지")
        public MultipartFile image;
    }

    @Operation(summary = "프로필 변경", description = "본인의 프로필(이름/이미지)을 변경합니다. 본인만 변경할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류(본인만 변경 가능)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = UpdateMultipartRequest.class),
            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
    ))
    ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "사용자 ID", required = true) @PathVariable UUID userId,
            @Parameter(hidden = true) UserUpdateRequest request,
            @Parameter(hidden = true) MultipartFile image);
}
