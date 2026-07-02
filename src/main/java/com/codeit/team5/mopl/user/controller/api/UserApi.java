package com.codeit.team5.mopl.user.controller.api;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.dto.suggestion.ErrorResponseSuggestion;
import com.codeit.team5.mopl.user.dto.request.ChangePasswordRequest;
import com.codeit.team5.mopl.user.dto.request.UserCursorRequest;
import com.codeit.team5.mopl.user.dto.request.UserLockedUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.request.UserRoleUpdateRequest;
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
            @Parameter(hidden = true) MoplUserDetails userDetails,
            @Parameter(description = "사용자 ID", required = true) @PathVariable UUID userId,
            @Parameter(hidden = true) UserUpdateRequest request,
            @Parameter(hidden = true) MultipartFile image);

    @Operation(
            summary = "[어드민] 사용자 권한 변경",
            description = "[어드민 기능] 관리자가 사용자의 권한을 USER 또는 ADMIN으로 변경합니다. 권한이 변경된 사용자는 재인증이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "권한 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류(관리자만 변경 가능)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "409", description = "이미 동일한 권한입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> updateRole(
            @Parameter(description = "권한을 변경할 사용자 ID", required = true)
            @PathVariable UUID userId,

            @Parameter(description = "변경할 사용자 권한", required = true)
            @Valid @RequestBody UserRoleUpdateRequest request
    );

    @Operation(
            summary = "[어드민] 계정 잠금 상태 변경",
            description = "[어드민 기능] 계정 잠금 상태를 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "잠금 상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류(관리자만 변경 가능)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "404", description = "사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "409", description = "이미 동일한 잠금 상태입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> updateLockStatus(
            @Parameter(description = "권한을 변경할 사용자 ID", required = true)
            @PathVariable UUID userId,

            @Parameter(description = "변경할 사용자 잠금 상태", required = true)
            @Valid @RequestBody UserLockedUpdateRequest request
    );

    @Operation(
            summary = "비밀번호 변경",
            description = "비밀번호를 변경합니다. 본인의 비밀번호만 변경할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류(본인만 변경 가능)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<Void> updatePassword(
            @Parameter(hidden = true) MoplPrincipal userDetails,

            @Parameter(description = "비밀번호를 변경할 사용자 Id", required = true)
            @PathVariable UUID userId,

            @Parameter(description = "변경할 비밀번호", required = true)
            @Valid @RequestBody ChangePasswordRequest request
    );

    @Operation(
            summary = "[어드민] 사용자 목록 조회",
            description = "[어드민 기능] 사용자의 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CursorResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "401", description = "인증 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "403", description = "권한 오류(관리자만 조회 가능)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponseSuggestion.class)))
    })
    ResponseEntity<CursorResponse<UserResponse>> getUsers(
            @Parameter(hidden = true)UserCursorRequest request
    );
}
